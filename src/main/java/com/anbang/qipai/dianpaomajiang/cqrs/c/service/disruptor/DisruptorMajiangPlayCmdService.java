package com.anbang.qipai.dianpaomajiang.cqrs.c.service.disruptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.MajiangActionResult;
import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.MajiangGameValueObject;
import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.ReadyToNextPanResult;
import com.anbang.qipai.dianpaomajiang.cqrs.c.service.MajiangPlayCmdService;
import com.anbang.qipai.dianpaomajiang.cqrs.c.service.impl.MajiangPlayCmdServiceImpl;
import com.highto.framework.concurrent.DeferredResult;
import com.highto.framework.ddd.CommonCommand;

@Component(value = "majiangPlayCmdService")
public class DisruptorMajiangPlayCmdService extends DisruptorCmdServiceBase implements MajiangPlayCmdService {

	@Autowired
	private MajiangPlayCmdServiceImpl majiangPlayCmdServiceImpl;

	@Override
	public MajiangActionResult action(String playerId, Integer actionId, Integer actionNo, Long actionTime)
			throws Exception {
		CommonCommand cmd = new CommonCommand(MajiangPlayCmdServiceImpl.class.getName(), "action", playerId, actionId,
				actionNo, actionTime);
		DeferredResult<MajiangActionResult> result = publishEvent(disruptorFactory.getCoreCmdDisruptor(), cmd, () -> {
			MajiangActionResult majiangActionResult = majiangPlayCmdServiceImpl.action(cmd.getParameter(),
					cmd.getParameter(), cmd.getParameter(), cmd.getParameter());
			return majiangActionResult;
		});
		try {
			return result.getResult();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public ReadyToNextPanResult readyToNextPan(String playerId) throws Exception {
		CommonCommand cmd = new CommonCommand(MajiangPlayCmdServiceImpl.class.getName(), "readyToNextPan", playerId);
		DeferredResult<ReadyToNextPanResult> result = publishEvent(disruptorFactory.getCoreCmdDisruptor(), cmd, () -> {
			ReadyToNextPanResult readyToNextPanResult = majiangPlayCmdServiceImpl.readyToNextPan(cmd.getParameter());
			return readyToNextPanResult;
		});
		try {
			return result.getResult();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public MajiangGameValueObject xipai(String playerId) throws Exception {
		CommonCommand cmd = new CommonCommand(MajiangPlayCmdServiceImpl.class.getName(), "xipai", playerId);
		DeferredResult<MajiangGameValueObject> result = publishEvent(disruptorFactory.getCoreCmdDisruptor(), cmd,
				() -> {
					MajiangGameValueObject majiangGameValueObject = majiangPlayCmdServiceImpl.xipai(cmd.getParameter());
					return majiangGameValueObject;
				});
		try {
			return result.getResult();
		} catch (Exception e) {
			throw e;
		}
	}
}
