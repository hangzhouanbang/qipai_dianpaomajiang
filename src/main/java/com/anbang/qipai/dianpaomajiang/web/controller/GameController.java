package com.anbang.qipai.dianpaomajiang.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.MajiangGameValueObject;
import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.ReadyForGameResult;
import com.anbang.qipai.dianpaomajiang.cqrs.c.service.GameCmdService;
import com.anbang.qipai.dianpaomajiang.cqrs.c.service.PlayerAuthService;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.GameFinishVoteDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.JuResultDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.MajiangGameDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.MajiangGamePlayerDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.PanActionFrameDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.dbo.PanResultDbo;
import com.anbang.qipai.dianpaomajiang.cqrs.q.service.MajiangGameQueryService;
import com.anbang.qipai.dianpaomajiang.cqrs.q.service.MajiangPlayQueryService;
import com.anbang.qipai.dianpaomajiang.msg.msjobj.MajiangHistoricalJuResult;
import com.anbang.qipai.dianpaomajiang.msg.service.DianpaoMajiangGameMsgService;
import com.anbang.qipai.dianpaomajiang.msg.service.DianpaoMajiangResultMsgService;
import com.anbang.qipai.dianpaomajiang.msg.service.MemberGoldsMsgService;
import com.anbang.qipai.dianpaomajiang.msg.service.WatchRecordMsgService;
import com.anbang.qipai.dianpaomajiang.msg.service.WiseCrackMsgServcie;
import com.anbang.qipai.dianpaomajiang.plan.bean.MemberGoldBalance;
import com.anbang.qipai.dianpaomajiang.plan.bean.PlayerInfo;
import com.anbang.qipai.dianpaomajiang.plan.service.MemberGoldBalanceService;
import com.anbang.qipai.dianpaomajiang.plan.service.PlayerInfoService;
import com.anbang.qipai.dianpaomajiang.utils.CommonVoUtil;
import com.anbang.qipai.dianpaomajiang.web.vo.CommonVO;
import com.anbang.qipai.dianpaomajiang.web.vo.GameFinishVoteVO;
import com.anbang.qipai.dianpaomajiang.web.vo.GameVO;
import com.anbang.qipai.dianpaomajiang.web.vo.PanActionFrameVO;
import com.anbang.qipai.dianpaomajiang.web.vo.PanResultVO;
import com.anbang.qipai.dianpaomajiang.websocket.GamePlayWsNotifier;
import com.anbang.qipai.dianpaomajiang.websocket.QueryScope;
import com.anbang.qipai.dianpaomajiang.websocket.WatchQueryScope;
import com.dml.mpgame.game.Canceled;
import com.dml.mpgame.game.CrowdLimitsException;
import com.dml.mpgame.game.Finished;
import com.dml.mpgame.game.GameNotFoundException;
import com.dml.mpgame.game.Playing;
import com.dml.mpgame.game.extend.fpmpv.VoteNotPassWhenWaitingNextPan;
import com.dml.mpgame.game.extend.vote.FinishedByVote;
import com.dml.mpgame.game.extend.vote.VoteNotPassWhenPlaying;
import com.dml.mpgame.game.player.GamePlayerOnlineState;
import com.dml.mpgame.game.watch.WatchRecord;
import com.dml.mpgame.game.watch.Watcher;

@RestController
@RequestMapping("/game")
public class GameController {

	@Autowired
	private GameCmdService gameCmdService;

	@Autowired
	private MajiangGameQueryService majiangGameQueryService;

	@Autowired
	private MajiangPlayQueryService majiangPlayQueryService;

	@Autowired
	private PlayerAuthService playerAuthService;

	@Autowired
	private GamePlayWsNotifier wsNotifier;

	@Autowired
	private DianpaoMajiangGameMsgService gameMsgService;

	@Autowired
	private DianpaoMajiangResultMsgService dianpaoMajiangResultMsgService;

	@Autowired
	private MemberGoldBalanceService memberGoldBalanceService;

	@Autowired
	private MemberGoldsMsgService memberGoldsMsgService;

	@Autowired
	private PlayerInfoService playerInfoService;

	@Autowired
	private WiseCrackMsgServcie wiseCrackMsgServcie;

	@Autowired
	private WatchRecordMsgService watchRecordMsgService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * 新一局游戏
	 */
	@RequestMapping(value = "/newgame")
	@ResponseBody
	public CommonVO newgame(String playerId, int panshu, int renshu, boolean dianpao, boolean dapao,
			boolean quzhongfabai, boolean zhuaniao, int niaoshu, boolean qingyise) {
		CommonVO vo = new CommonVO();
		String newGameId = UUID.randomUUID().toString();
		MajiangGameValueObject majiangGameValueObject = gameCmdService.newMajiangGame(newGameId, playerId, panshu,
				renshu, dianpao, dapao, quzhongfabai, zhuaniao, niaoshu, qingyise);
		majiangGameQueryService.newMajiangGame(majiangGameValueObject);
		String token = playerAuthService.newSessionForPlayer(playerId);
		Map data = new HashMap();
		data.put("gameId", newGameId);
		data.put("token", token);
		vo.setData(data);
		gameMsgService.newSessionForPlayer(playerId, token, newGameId);
		return vo;
	}

	/**
	 * 新一局游戏,游戏未开始时离开就是退出
	 */
	@RequestMapping(value = "/newgame_leave_quit")
	@ResponseBody
	public CommonVO newgame_leave_quit(String playerId, int panshu, int renshu, boolean dianpao, boolean dapao,
			boolean quzhongfabai, boolean zhuaniao, int niaoshu, boolean qingyise) {
		CommonVO vo = new CommonVO();
		String newGameId = UUID.randomUUID().toString();
		MajiangGameValueObject majiangGameValueObject = gameCmdService.newMajiangGameLeaveAndQuit(newGameId, playerId,
				panshu, renshu, dianpao, dapao, quzhongfabai, zhuaniao, niaoshu, qingyise);
		majiangGameQueryService.newMajiangGame(majiangGameValueObject);
		String token = playerAuthService.newSessionForPlayer(playerId);
		Map data = new HashMap();
		data.put("gameId", newGameId);
		data.put("token", token);
		vo.setData(data);
		gameMsgService.newSessionForPlayer(playerId, token, newGameId);
		return vo;
	}

	/**
	 * 新一局游戏,游戏未开始时退出就不在房间内
	 */
	@RequestMapping(value = "/newgame_player_quit")
	@ResponseBody
	public CommonVO newgame_player_quit(String playerId, int panshu, int renshu, boolean dianpao, boolean dapao,
			boolean quzhongfabai, boolean zhuaniao, int niaoshu, boolean qingyise) {
		CommonVO vo = new CommonVO();
		String newGameId = UUID.randomUUID().toString();
		MajiangGameValueObject majiangGameValueObject = gameCmdService.newMajiangGamePlayerLeaveAndQuit(newGameId,
				playerId, panshu, renshu, dianpao, dapao, quzhongfabai, zhuaniao, niaoshu, qingyise);
		majiangGameQueryService.newMajiangGame(majiangGameValueObject);
		String token = playerAuthService.newSessionForPlayer(playerId);
		Map data = new HashMap();
		data.put("gameId", newGameId);
		data.put("token", token);
		vo.setData(data);
		gameMsgService.newSessionForPlayer(playerId, token, newGameId);
		return vo;
	}

	/**
	 * 加入游戏
	 */
	@RequestMapping(value = "/joingame")
	@ResponseBody
	public CommonVO joingame(String playerId, String gameId) {
		CommonVO vo = new CommonVO();
		MajiangGameValueObject majiangGameValueObject;
		try {
			majiangGameValueObject = gameCmdService.joinGame(playerId, gameId);
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().toString());
			return vo;
		}
		majiangGameQueryService.joinGame(majiangGameValueObject);
		// 通知其他人
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				wsNotifier.notifyToQuery(otherPlayerId, QueryScope.scopesForState(majiangGameValueObject.getState(),
						majiangGameValueObject.findPlayerState(otherPlayerId)));
			}
		}
		String token = playerAuthService.newSessionForPlayer(playerId);
		Map data = new HashMap();
		data.put("token", token);
		vo.setData(data);
		gameMsgService.newSessionForPlayer(playerId, token, gameId);
		return vo;
	}

	/**
	 * 加入观战
	 */
	@RequestMapping(value = "/joinwatch")
	@ResponseBody
	public CommonVO joinWatch(String playerId, String gameId) {
		MajiangGameValueObject majiangGameValueObject;
		String nickName = "";
		String headimgurl = "";

		// 加入观战
		try {
			PlayerInfo playerInfo = playerInfoService.findPlayerInfoById(playerId);
			nickName = playerInfo.getNickname();
			headimgurl = playerInfo.getHeadimgurl();
			majiangGameValueObject = gameCmdService.joinWatch(playerId, nickName, headimgurl, gameId);
		} catch (CrowdLimitsException e) {
			return CommonVoUtil.error("too many watchers");
		} catch (Exception e) {
			logger.error("joinWatch:" + JSON.toJSONString(e));
			return CommonVoUtil.error(e.getClass().toString());
		}

		// 通知游戏玩家
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			wsNotifier.notifyWatchInfo(otherPlayerId, "input", playerId, nickName, headimgurl);
		}
		// 通知其他观战者
		Map<String, Watcher> map = gameCmdService.getwatch(gameId);
		if (!CollectionUtils.isEmpty(map)) {
			for (Watcher list : map.values()) {
				if (!list.getId().equals(playerId)) {
					wsNotifier.notifyWatchInfo(list.getId(), "input", playerId, nickName, headimgurl);
				}
			}
		}

		// 返回查询token
		String token = playerAuthService.newSessionForPlayer(playerId);

		Watcher watcher = new Watcher();
		watcher.setId(playerId);
		watcher.setHeadimgurl(headimgurl);
		watcher.setNickName(nickName);
		watcher.setState("join");
		watcher.setJoinTime(System.currentTimeMillis());
		WatchRecord watchRecord = majiangGameQueryService.saveWatchRecord(gameId, watcher);
		watchRecordMsgService.joinWatch(watchRecord);

		Map data = new HashMap();
		data.put("token", token);
		gameMsgService.newSessionForPlayer(playerId, token, gameId);
		return CommonVoUtil.success(data, "join watch success");
	}

	/**
	 * 离开观战
	 */
	@RequestMapping(value = "/leavewatch")
	@ResponseBody
	public CommonVO leaveWatch(String token, String gameId) {
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			return CommonVoUtil.error("invalid token");
		}
		MajiangGameValueObject majiangGameValueObject;
		String nickName = "";
		String headimgurl = "";

		try {
			nickName = playerInfoService.findPlayerInfoById(playerId).getNickname();
			majiangGameValueObject = gameCmdService.leaveWatch(playerId, gameId);
		} catch (Exception e) {
			logger.error("leavewatch():" + gameId + JSON.toJSONString(e));
			return CommonVoUtil.error(e.getClass().toString());
		}

		// 通知游戏玩家
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			wsNotifier.notifyWatchInfo(otherPlayerId, "leave", playerId, nickName, headimgurl);
		}
		// 通知观战者
		Map<String, Watcher> map = gameCmdService.getwatch(gameId);
		if (!CollectionUtils.isEmpty(map)) {
			for (Watcher list : map.values()) {
				if (!list.getId().equals(playerId)) {
					wsNotifier.notifyWatchInfo(list.getId(), "input", playerId, nickName, headimgurl);
				}
			}
		}

		Watcher watcher = new Watcher();
		watcher.setId(playerId);
		watcher.setHeadimgurl(headimgurl);
		watcher.setNickName(nickName);
		watcher.setState("leave");
		WatchRecord watchRecord = majiangGameQueryService.saveWatchRecord(gameId, watcher);
		watchRecordMsgService.leaveWatch(watchRecord);

		return CommonVoUtil.success("leave success");
	}

	/**
	 * 查询正在观战的玩家
	 */
	@RequestMapping(value = "/queryWatch")
	@ResponseBody
	public CommonVO queryWatch(String gameId) {
		Map<String, Watcher> map = gameCmdService.getwatch(gameId);
		if (CollectionUtils.isEmpty(map)) {
			return CommonVoUtil.success("queryWatch success");
		}
		return CommonVoUtil.success(map.values(), "queryWatch success");
	}

	/**
	 * 观战者看到的信息
	 */
	@RequestMapping(value = "/watchinginfo")
	@ResponseBody
	public CommonVO watchingInfo(String gameId) {
		CommonVO vo = new CommonVO();
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		GameVO gameVO = new GameVO(majiangGameDbo);
		Map data = new HashMap();
		data.put("game", gameVO);
		vo.setData(data);
		return vo;
	}

	/**
	 * 挂起（手机按黑的时候调用）
	 */
	@RequestMapping(value = "/hangup")
	@ResponseBody
	public CommonVO hangup(String token) {
		CommonVO vo = new CommonVO();
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		MajiangGameValueObject majiangGameValueObject;
		String endFlag = "query";
		try {
			majiangGameValueObject = gameCmdService.leaveGameByHangup(playerId);
			if (majiangGameValueObject == null) {
				vo.setSuccess(true);
				return vo;
			}
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		majiangGameQueryService.leaveGame(majiangGameValueObject);
		// 断开玩家的socket
		wsNotifier.closeSessionForPlayer(playerId);
		String gameId = majiangGameValueObject.getId();
		JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
		// 记录战绩
		if (juResultDbo != null) {
			MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
			MajiangHistoricalJuResult juResult = new MajiangHistoricalJuResult(juResultDbo, majiangGameDbo);
			dianpaoMajiangResultMsgService.recordJuResult(juResult);
		}
		if (majiangGameValueObject.getState().name().equals(FinishedByVote.name)
				|| majiangGameValueObject.getState().name().equals(Canceled.name)) {
			gameMsgService.gameFinished(gameId);
			endFlag = WatchQueryScope.watchEnd.name();
		} else {
			gameMsgService.gamePlayerLeave(majiangGameValueObject, playerId);

		}
		// 通知其他玩家
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				List<QueryScope> scopes = QueryScope.scopesForState(majiangGameValueObject.getState(),
						majiangGameValueObject.findPlayerState(otherPlayerId));
				scopes.remove(QueryScope.panResult);
				if (majiangGameValueObject.getState().name().equals(VoteNotPassWhenPlaying.name)
						|| majiangGameValueObject.getState().name().equals(VoteNotPassWhenWaitingNextPan.name)) {
					scopes.remove(QueryScope.gameFinishVote);
				}
				wsNotifier.notifyToQuery(otherPlayerId, scopes);
			}
		}

		hintWatcher(gameId, endFlag);
		return vo;
	}

	/**
	 * 离开游戏(非退出,还会回来的)
	 */
	@RequestMapping(value = "/leavegame")
	@ResponseBody
	public CommonVO leavegame(String token) {
		CommonVO vo = new CommonVO();
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		MajiangGameValueObject majiangGameValueObject;
		String endFlag = "query";
		try {
			majiangGameValueObject = gameCmdService.leaveGame(playerId);
			if (majiangGameValueObject == null) {
				vo.setSuccess(true);
				return vo;
			}
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		majiangGameQueryService.leaveGame(majiangGameValueObject);
		// 断开玩家的socket
		wsNotifier.closeSessionForPlayer(playerId);
		String gameId = majiangGameValueObject.getId();
		JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
		// 记录战绩
		if (juResultDbo != null) {
			MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
			MajiangHistoricalJuResult juResult = new MajiangHistoricalJuResult(juResultDbo, majiangGameDbo);
			dianpaoMajiangResultMsgService.recordJuResult(juResult);
		}
		if (majiangGameValueObject.getState().name().equals(FinishedByVote.name)
				|| majiangGameValueObject.getState().name().equals(Canceled.name)) {
			gameMsgService.gameFinished(gameId);
			endFlag = WatchQueryScope.watchEnd.name();
		} else if (majiangGameValueObject.getState().name().equals(Finished.name)) {
			gameMsgService.gameCanceled(gameId, playerId);
		} else {
			gameMsgService.gamePlayerLeave(majiangGameValueObject, playerId);

		}
		// 通知其他玩家
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				List<QueryScope> scopes = QueryScope.scopesForState(majiangGameValueObject.getState(),
						majiangGameValueObject.findPlayerState(otherPlayerId));
				if (!majiangGameValueObject.getState().name().equals(Finished.name)) {
					scopes.remove(QueryScope.panResult);
				}
				if (majiangGameValueObject.getState().name().equals(VoteNotPassWhenPlaying.name)
						|| majiangGameValueObject.getState().name().equals(VoteNotPassWhenWaitingNextPan.name)) {
					scopes.remove(QueryScope.gameFinishVote);
				}
				wsNotifier.notifyToQuery(otherPlayerId, scopes);
			}
		}

		hintWatcher(gameId, endFlag);
		return vo;
	}

	/**
	 * 返回游戏
	 */
	@RequestMapping(value = "/backtogame")
	@ResponseBody
	public CommonVO backtogame(String playerId, String gameId) {
		// 是观战返回新token
		Map<String, Watcher> map = gameCmdService.getwatch(gameId);
		if (!CollectionUtils.isEmpty(map) && map.containsKey(playerId)) {
			List<String> playerIds = new ArrayList<>();
			playerIds.add(playerId);
			wsNotifier.notifyToWatchQuery(playerIds, "query");

			Map data = new HashMap();
			String token = playerAuthService.newSessionForPlayer(playerId);
			data.put("token", token);
			return CommonVoUtil.success(data, "backtogame success");
		}

		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		MajiangGameValueObject majiangGameValueObject;
		try {
			majiangGameValueObject = gameCmdService.backToGame(playerId, gameId);
		} catch (Exception e) {
			// 如果找不到game，看下是否是已经结束(正常结束和被投票)的game
			if (e instanceof GameNotFoundException) {
				MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
				if (majiangGameDbo != null && (majiangGameDbo.getState().name().equals(FinishedByVote.name)
						|| majiangGameDbo.getState().name().equals(Finished.name))) {
					data.put("queryScope", QueryScope.juResult);
					return vo;
				}
			}
			vo.setSuccess(false);
			vo.setMsg(e.getClass().toString());
			return vo;
		}

		majiangGameQueryService.backToGame(playerId, majiangGameValueObject);

		// 通知其他人
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				List<QueryScope> scopes = QueryScope.scopesForState(majiangGameValueObject.getState(),
						majiangGameValueObject.findPlayerState(otherPlayerId));
				scopes.remove(QueryScope.panResult);
				if (majiangGameValueObject.getState().name().equals(VoteNotPassWhenPlaying.name)
						|| majiangGameValueObject.getState().name().equals(VoteNotPassWhenWaitingNextPan.name)) {
					scopes.remove(QueryScope.gameFinishVote);
				}
				wsNotifier.notifyToQuery(otherPlayerId, scopes);
			}
		}

		String token = playerAuthService.newSessionForPlayer(playerId);
		data.put("token", token);
		gameMsgService.newSessionForPlayer(playerId, token, gameId);
		return vo;
	}

	/**
	 * 游戏的所有信息,不包含局
	 * 
	 * @param gameId
	 * @return
	 */
	@RequestMapping(value = "/info")
	@ResponseBody
	public CommonVO info(String gameId) {
		CommonVO vo = new CommonVO();
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		GameVO gameVO = new GameVO(majiangGameDbo);
		Map data = new HashMap();
		data.put("game", gameVO);
		vo.setData(data);
		return vo;
	}

	/**
	 * 最开始的准备,不适用下一盘的准备
	 * 
	 * @param token
	 * @return
	 */
	@RequestMapping(value = "/ready")
	@ResponseBody
	public CommonVO ready(String token) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		ReadyForGameResult readyForGameResult;
		try {
			readyForGameResult = gameCmdService.readyForGame(playerId, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}

		try {
			majiangPlayQueryService.readyForGame(readyForGameResult);// TODO 一起点准备的时候可能有同步问题.要靠框架解决
		} catch (Throwable e) {
			vo.setSuccess(false);
			vo.setMsg(e.getMessage());
			return vo;
		}
		// 通知其他人
		for (String otherPlayerId : readyForGameResult.getMajiangGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				wsNotifier.notifyToQuery(otherPlayerId,
						QueryScope.scopesForState(readyForGameResult.getMajiangGame().getState(),
								readyForGameResult.getMajiangGame().findPlayerState(otherPlayerId)));
			}
		}

		List<QueryScope> queryScopes = new ArrayList<>();
		queryScopes.add(QueryScope.gameInfo);
		if (readyForGameResult.getMajiangGame().getState().name().equals(Playing.name)) {
			queryScopes.add(QueryScope.panForMe);
			gameMsgService.start(readyForGameResult.getMajiangGame().getId());
		}
		data.put("queryScopes", queryScopes);
		return vo;
	}

	/**
	 * 最开始的取消准备,不适用下一盘的准备
	 *
	 * @param token
	 * @return
	 */
	@RequestMapping(value = "/cancelready")
	@ResponseBody
	public CommonVO cancelReady(String token) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		ReadyForGameResult readyForGameResult;
		try {
			readyForGameResult = gameCmdService.cancelReadyForGame(playerId, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}

		try {
			majiangPlayQueryService.readyForGame(readyForGameResult);// TODO 一起点准备的时候可能有同步问题.要靠框架解决
		} catch (Throwable e) {
			vo.setSuccess(false);
			vo.setMsg(e.getMessage());
			return vo;
		}
		// 通知其他人
		for (String otherPlayerId : readyForGameResult.getMajiangGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				wsNotifier.notifyToQuery(otherPlayerId,
						QueryScope.scopesForState(readyForGameResult.getMajiangGame().getState(),
								readyForGameResult.getMajiangGame().findPlayerState(otherPlayerId)));

			}
		}

		List<QueryScope> queryScopes = new ArrayList<>();
		queryScopes.add(QueryScope.gameInfo);
		if (readyForGameResult.getMajiangGame().getState().name().equals(Playing.name)) {
			queryScopes.add(QueryScope.panForMe);
		}
		data.put("queryScopes", queryScopes);
		return vo;
	}

	@RequestMapping(value = "/finish")
	@ResponseBody
	public CommonVO finish(String token) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		MajiangGameValueObject majiangGameValueObject;
		try {
			majiangGameValueObject = gameCmdService.finish(playerId, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		majiangGameQueryService.finish(majiangGameValueObject);
		String gameId = majiangGameValueObject.getId();
		JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
		// 记录战绩
		if (juResultDbo != null) {
			MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
			MajiangHistoricalJuResult juResult = new MajiangHistoricalJuResult(juResultDbo, majiangGameDbo);
			dianpaoMajiangResultMsgService.recordJuResult(juResult);
		}

		if (majiangGameValueObject.getState().name().equals(FinishedByVote.name)
				|| majiangGameValueObject.getState().name().equals(Canceled.name)) {
			gameMsgService.gameFinished(gameId);
			data.put("queryScope", QueryScope.gameInfo);
		} else {
			// 游戏没结束有两种可能：一种是发起了投票。还有一种是游戏没开始，解散发起人又不是房主，那就自己走人。
			if (majiangGameValueObject.allPlayerIds().contains(playerId)) {
				data.put("queryScope", QueryScope.gameFinishVote);
			} else {
				data.put("queryScope", null);
				gameMsgService.gamePlayerLeave(majiangGameValueObject, playerId);
			}
		}
		// 通知其他人来查询
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = majiangGameValueObject.findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					List<QueryScope> scopes = QueryScope.scopesForState(majiangGameValueObject.getState(),
							majiangGameValueObject.findPlayerState(otherPlayerId));
					scopes.remove(QueryScope.panResult);
					wsNotifier.notifyToQuery(otherPlayerId, scopes);
				}
			}
		}

		return vo;
	}

	@RequestMapping(value = "/vote_to_finish")
	@ResponseBody
	public CommonVO votetofinish(String token, boolean yes) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		MajiangGameValueObject majiangGameValueObject;
		String endFlag = "query";
		try {
			majiangGameValueObject = gameCmdService.voteToFinish(playerId, yes);
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		String gameId = majiangGameValueObject.getId();
		majiangGameQueryService.voteToFinish(majiangGameValueObject);
		JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
		// 记录战绩
		if (juResultDbo != null) {
			MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
			MajiangHistoricalJuResult juResult = new MajiangHistoricalJuResult(juResultDbo, majiangGameDbo);
			dianpaoMajiangResultMsgService.recordJuResult(juResult);
		}
		if (majiangGameValueObject.getState().name().equals(FinishedByVote.name)
				|| majiangGameValueObject.getState().name().equals(Canceled.name)) {
			gameMsgService.gameFinished(gameId);
			endFlag = WatchQueryScope.watchEnd.name();
		}

		data.put("queryScope", QueryScope.gameFinishVote);
		// 通知其他人来查询投票情况
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = majiangGameValueObject.findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					List<QueryScope> scopes = QueryScope.scopesForState(majiangGameValueObject.getState(),
							majiangGameValueObject.findPlayerState(otherPlayerId));
					scopes.remove(QueryScope.panResult);
					wsNotifier.notifyToQuery(otherPlayerId, scopes);
				}
			}
		}

		hintWatcher(gameId, endFlag);
		return vo;
	}

	/**
	 * 投票倒计时结束弃权
	 */
	@RequestMapping(value = "/timeover_to_waiver")
	@ResponseBody
	public CommonVO timeoverToWaiver(String token) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		MajiangGameValueObject majiangGameValueObject;
		String endFlag = "query";
		try {
			majiangGameValueObject = gameCmdService.voteToFinishByTimeOver(playerId, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		String gameId = majiangGameValueObject.getId();
		majiangGameQueryService.voteToFinish(majiangGameValueObject);
		JuResultDbo juResultDbo = majiangPlayQueryService.findJuResultDbo(gameId);
		// 记录战绩
		if (juResultDbo != null) {
			MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
			MajiangHistoricalJuResult juResult = new MajiangHistoricalJuResult(juResultDbo, majiangGameDbo);
			dianpaoMajiangResultMsgService.recordJuResult(juResult);
		}
		if (majiangGameValueObject.getState().name().equals(FinishedByVote.name)
				|| majiangGameValueObject.getState().name().equals(Canceled.name)) {
			gameMsgService.gameFinished(gameId);
			endFlag = WatchQueryScope.watchEnd.name();
		}

		data.put("queryScope", QueryScope.gameFinishVote);
		// 通知其他人来查询投票情况
		for (String otherPlayerId : majiangGameValueObject.allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = majiangGameValueObject.findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					List<QueryScope> scopes = QueryScope.scopesForState(majiangGameValueObject.getState(),
							majiangGameValueObject.findPlayerState(otherPlayerId));
					scopes.remove(QueryScope.panResult);
					wsNotifier.notifyToQuery(otherPlayerId, scopes);
				}
			}
		}

		hintWatcher(gameId, endFlag);
		return vo;

	}

	@RequestMapping(value = "/finish_vote_info")
	@ResponseBody
	public CommonVO finishvoteinfo(String gameId) {

		CommonVO vo = new CommonVO();
		GameFinishVoteDbo gameFinishVoteDbo = majiangGameQueryService.findGameFinishVoteDbo(gameId);
		Map data = new HashMap();
		data.put("vote", new GameFinishVoteVO(gameFinishVoteDbo.getVote()));
		vo.setData(data);
		return vo;
	}

	@RequestMapping(value = "/wisecrack")
	@ResponseBody
	public CommonVO wisecrack(String token, String gameId, String ordinal) {
		CommonVO vo = new CommonVO();
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		PlayerInfo playerInfo = playerInfoService.findPlayerInfoById(playerId);
		if (playerInfo.isVip() || !ordinal.contains("qiaopihuafy")) {
			// 通知其他人
			for (MajiangGamePlayerDbo otherPlayer : majiangGameDbo.getPlayers()) {
				if (!otherPlayer.getPlayerId().equals(playerId)) {
					wsNotifier.notifyToListenWisecrack(otherPlayer.getPlayerId(), ordinal, playerId);
				}
			}
			wiseCrackMsgServcie.wisecrack(playerId);
			vo.setSuccess(true);
			return vo;
		}
		MemberGoldBalance account = memberGoldBalanceService.findByMemberId(playerId);
		if (account.getBalanceAfter() > 10) {
			memberGoldsMsgService.withdraw(playerId, 10, "wisecrack");
			// 通知其他人
			for (MajiangGamePlayerDbo otherPlayer : majiangGameDbo.getPlayers()) {
				if (!otherPlayer.getPlayerId().equals(playerId)) {
					wsNotifier.notifyToListenWisecrack(otherPlayer.getPlayerId(), ordinal, playerId);
				}
			}
			wiseCrackMsgServcie.wisecrack(playerId);
			vo.setSuccess(true);
			return vo;
		}
		vo.setSuccess(false);
		vo.setMsg("InsufficientBalanceException");
		return vo;
	}

	@RequestMapping(value = "/playback")
	@ResponseBody
	public CommonVO playback(String gameId, int panNo) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		List<PanActionFrameDbo> frameList = majiangPlayQueryService.findPanActionFrameDboForBackPlay(gameId, panNo);
		List<PanActionFrameVO> frameVOList = new ArrayList<>();
		for (PanActionFrameDbo frame : frameList) {
			frame.getPanActionFrame().getPanAfterAction().getAvaliablePaiList().setPaiList(null);
			frameVOList.add(new PanActionFrameVO(frame.getPanActionFrame()));
		}
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		majiangGameDbo.setPanNo(panNo);
		GameVO gameVO = new GameVO(majiangGameDbo);
		PanResultDbo panResultDbo = majiangPlayQueryService.findPanResultDbo(gameId, panNo);
		data.put("panResult", new PanResultVO(panResultDbo, majiangGameDbo));
		data.put("game", gameVO);
		data.put("framelist", frameVOList);
		return vo;
	}

	@RequestMapping(value = "/speak")
	@ResponseBody
	public CommonVO speak(String token, String gameId, String wordId) {
		CommonVO vo = new CommonVO();
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		MajiangGameDbo majiangGameDbo = majiangGameQueryService.findMajiangGameDboById(gameId);
		List<MajiangGamePlayerDbo> playerList = majiangGameDbo.getPlayers();
		for (MajiangGamePlayerDbo player : playerList) {
			if (!player.getPlayerId().equals(playerId)) {
				wsNotifier.notifyToListenSpeak(player.getPlayerId(), wordId, playerId, true);
			}
		}

		// 观战者接收语音
		Map<String, Object> map = gameCmdService.getwatch(gameId);
		if (!CollectionUtils.isEmpty(map)) {
			List<String> playerIds = map.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList());
			for (String list : playerIds) {
				if (!list.equals(playerId)) {
					wsNotifier.notifyToListenSpeak(list, wordId, playerId, false);
				}
			}
		}

		vo.setSuccess(true);
		return vo;
	}

	/**
	 * 通知观战者
	 */
	private void hintWatcher(String gameId, String flag) {
		Map<String, Object> map = gameCmdService.getwatch(gameId);
		if (!CollectionUtils.isEmpty(map)) {
			List<String> playerIds = map.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList());
			wsNotifier.notifyToWatchQuery(playerIds, flag);
			if (WatchQueryScope.watchEnd.name().equals(flag)) {
				gameCmdService.recycleWatch(gameId);
			}
		}
	}
}
