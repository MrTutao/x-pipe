package com.ctrip.xpipe.redis.simple.latency;

import com.ctrip.xpipe.api.monitor.DelayMonitor;
import com.ctrip.xpipe.monitor.DefaultDelayMonitor;
import com.ctrip.xpipe.redis.keeper.impl.DefaultRedisSlave;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author wenchao.meng
 *
 * May 22, 2016 2:57:02 PM
 */
public class ReceiveMessageHandler extends ChannelDuplexHandler{
	
	private String runId;
	private long startOffset;
	private DelayMonitor delayMonitor = new DefaultDelayMonitor("CREATE_PSYNC", 50000);
	
	public ReceiveMessageHandler(String runId, long startOffset) {
		this.runId = runId;
		this.startOffset = startOffset;
		
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(String.format("psync %s %d\r\n", runId, startOffset).getBytes()));
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		ByteBuf source = (ByteBuf)msg;
		ByteBuf dst = ByteBufAllocator.DEFAULT.heapBuffer(source.readableBytes());
		source.readBytes(dst);
		
		long time = DefaultRedisSlave.getTime(dst);
		if(time > 0){
			delayMonitor.addData(time);
		}
		super.channelRead(ctx, msg);
	}

}
