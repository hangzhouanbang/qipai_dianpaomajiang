package com.anbang.qipai.dianpaomajiang.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.MajiangActionResult;
import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.ReadyToNextPanResult;
import com.anbang.qipai.dianpaomajiang.cqrs.c.service.MajiangPlayCmdService;
import com.anbang.qipai.dianpaomajiang.cqrs.c.service.PlayerAuthService;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.JuResultDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.MajiangGameDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.PanResultDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.service.MajiangGameQueryService;
import com.anbang.qipai.dianpaomajiang.cqrs.q.service.MajiangPlayQueryService;
import com.anbang.qipai.dianpaomajiang.msg.msjobj.MajiangHistoricalJuResult;
import com.anbang.qipai.dianpaomajiang.msg.msjobj.MajiangHistoricalPanResult;
import com.anbang.qipai.dianpaomajiang.msg.service.DianpaoMajiangGameMsgService;
import com.anbang.qipai.dianpaomajiang.msg.service.DianpaoMajiangResultMsgService;
import com.anbang.qipai.dianpaomajiang.web.vo.CommonVO;
import com.anbang.qipai.dianpaomajiang.web.vo.JuResultVO;
import com.anbang.qipai.dianpaomajiang.web.vo.PanActionFrameVO;
import com.anbang.qipai.dianpaomajiang.web.vo.PanResultVO;
import com.anbang.qipai.dianpaomajiang.websocket.GamePlayWsNotifier;
import com.anbang.qipai.dianpaomajiang.websocket.QueryScope;
import com.dml.majiang.pan.frame.PanActionFrame;

@RestController
@RequestMapping("/mj")
public class MajiangController {

	@Autowired
	private MajiangPlayCmdService majiangPlayCmdService;

	@Autowired
	private MajiangPlayQueryService majiangPlayQueryService;

	@Autowired
	private MajiangGameQueryService majiangGameQueryService;

	@Autowired
	private PlayerAuthService playerAuthService;

	@Autowired
	private GamePlayWsNotifier wsNotifier;

	@Autowired
	private DianpaoMajiangResultMsgService dianpaoMajiangResultMsgService;

	@Autowired
	private DianpaoMajiangGameMsgService gameMsgService;

	/**
	 * 当前盘我应该看到的所有信息
	 * 
	 * @param token
	 * @return
	 */
	@RequestMapping(value = "/pan_action_frame_for_me")
	@ResponseBody
	public CommonVO panactionframeforme(String token, String gameId) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		PanActionFrame panActionFrame;
		try {
			panActionFrame = majiangPlayQueryService.findAndFilterCurrentPanValueObjectForPlayer(gameId, playerId);
		} catch (Exception e) {
			e.printStackTrace();
			vo.setSuccess(false);
			vo.setMsg(e.getMessage());
			return vo;
		}
		data.put("panActionFrame", new PanActionFrameVO(panActionFrame));
		return vo;
	}

	/**
	 * @param gameId
	 * @param panNo
	 *            0代表不知道盘号，那么就取最新的一盘
	 * @return
	 */
	@RequestMapping(value = "/pan_result")
	@ResponseBody
	public CommonVO panresult(String gameId, int panNo) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		if (panNo == 0) {
			panNo = majiangGameDbo.getPanNo();
		}
		PanResultDbo panResultDbo = majiangPlayQueryService.findPanResultDbo(gameId, panNo);
		data.put("panResult", new PanResultVO(panResultDbo, majiangGameDbo));
		return vo;
	}

	@RequestMapping(value = "/ju_result")
	@ResponseBody
	public CommonVO juresult(String gameId) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
		data.put("juResult", new JuResultVO(juResultDbo, majiangGameDbo));
		return vo;
	}

	/**
	 * 麻将行牌
	 * 
	 * @param token
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/action")
	@ResponseBody
	public CommonVO action(String token, int id) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		List<String> queryScopes = new ArrayList<>();
		data.put("queryScopes", queryScopes);
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		MajiangActionResult majiangActionResult;
		try {
			majiangActionResult = majiangPlayCmdService.action(playerId, id, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		try {
			majiangPlayQueryService.action(majiangActionResult);
		} catch (Throwable e) {
			vo.setSuccess(false);
			vo.setMsg(e.getMessage());
			return vo;
		}

		if (majiangActionResult.getPanResult() == null) {// 盘没结束
			queryScopes.add(QueryScope.panForMe.name());
		} else {// 盘结束了
			String gameId = majiangActionResult.getMajiangGame().getId();
			MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
			if (majiangActionResult.getJuResult() != null) {// 局也结束了

				JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
				MajiangHistoricalJuResult juResult = new MajiangHistoricalJuResult(juResultDbo, majiangGameDbo);
				dianpaoMajiangResultMsgService.recordJuResult(juResult);

				gameMsgService.gameFinished(gameId);
				queryScopes.add(QueryScope.juResult.name());
			} else {
				queryScopes.add(QueryScope.panResult.name());
				queryScopes.add(QueryScope.gameInfo.name());
			}
			PanResultDbo panResultDbo = majiangPlayQueryService.findPanResultDbo(gameId,
					majiangActionResult.getPanResult().getPan().getNo());
			MajiangHistoricalPanResult panResult = new MajiangHistoricalPanResult(panResultDbo, majiangGameDbo);
			dianpaoMajiangResultMsgService.recordPanResult(panResult);
			gameMsgService.panFinished(majiangActionResult.getMajiangGame(),
					majiangActionResult.getPanActionFrame().getPanAfterAction());

		}

		// 通知其他人
		for (String otherPlayerId : majiangActionResult.getMajiangGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				QueryScope
						.scopesForState(majiangActionResult.getMajiangGame().getState(),
								majiangActionResult.getMajiangGame().findPlayerState(otherPlayerId))
						.forEach((scope) -> {
							wsNotifier.notifyToQuery(otherPlayerId, scope.name());
						});
			}
		}

		return vo;
	}

	@RequestMapping(value = "/ready_to_next_pan")
	@ResponseBody
	public CommonVO readytonextpan(String token) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		ReadyToNextPanResult readyToNextPanResult;
		try {
			readyToNextPanResult = majiangPlayCmdService.readyToNextPan(playerId);
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}

		try {
			majiangPlayQueryService.readyToNextPan(readyToNextPanResult);
		} catch (Throwable e) {
			vo.setSuccess(false);
			vo.setMsg(e.getMessage());
			return vo;
		}

		// 通知其他人
		PanActionFrame firstActionFrame = readyToNextPanResult.getFirstActionFrame();
		List<QueryScope> queryScopes = new ArrayList<>();
		queryScopes.add(QueryScope.gameInfo);
		if (firstActionFrame != null) {
			queryScopes.add(QueryScope.panForMe);
		}
		// 通知其他人
		for (String otherPlayerId : readyToNextPanResult.getMajiangGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				List<QueryScope> scopes = QueryScope.scopesForState(readyToNextPanResult.getMajiangGame().getState(),
						readyToNextPanResult.getMajiangGame().findPlayerState(otherPlayerId));
				scopes.remove(QueryScope.panResult);
				scopes.forEach((scope) -> {
					wsNotifier.notifyToQuery(otherPlayerId, scope.name());
				});
			}
		}
		data.put("queryScopes", queryScopes);
		return vo;
	}

}