package com.anbang.qipai.dianpaomajiang.cqrs.c.service;

import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.MajiangGameValueObject;
import com.anbang.qipai.dianpaomajiang.cqrs.c.domain.ReadyForGameResult;
import com.dml.mpgame.game.GameValueObject;

public interface GameCmdService {

	MajiangGameValueObject newMajiangGame(String gameId, String playerId, Integer panshu, Integer renshu,
			Boolean dianpao, Boolean dapao, Boolean quzhongfabai, Boolean zhuaniao, Integer niaoshu, Boolean qingyise);

	MajiangGameValueObject joinGame(String playerId, String gameId) throws Exception;

	MajiangGameValueObject leaveGame(String playerId) throws Exception;

	MajiangGameValueObject backToGame(String playerId, String gameId) throws Exception;

	ReadyForGameResult readyForGame(String playerId, Long currentTime) throws Exception;

	ReadyForGameResult cancelReadyForGame(String playerId, Long currentTime) throws Exception;

	MajiangGameValueObject finish(String playerId, Long currentTime) throws Exception;

	MajiangGameValueObject voteToFinish(String playerId, Boolean yes) throws Exception;

	MajiangGameValueObject voteToFinishByTimeOver(String playerId, Long currentTime) throws Exception;

	void bindPlayer(String playerId, String gameId);

	GameValueObject finishGameImmediately(String gameId) throws Exception;

	MajiangGameValueObject leaveGameByOffline(String playerId) throws Exception;

	MajiangGameValueObject leaveGameByHangup(String playerId) throws Exception;
}
