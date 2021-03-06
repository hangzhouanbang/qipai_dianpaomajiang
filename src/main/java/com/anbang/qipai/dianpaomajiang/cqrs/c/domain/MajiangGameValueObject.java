package com.anbang.qipai.dianpaomajiang.cqrs.c.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.dml.majiang.ju.result.JuResult;
import com.dml.mpgame.game.extend.fpmpv.FixedPlayersMultipanAndVotetofinishGameValueObject;

public class MajiangGameValueObject extends FixedPlayersMultipanAndVotetofinishGameValueObject {

	private int panshu;
	private int renshu;
	private boolean dianpao;
	private boolean dapao;
	private boolean quzhongfabai;
	private boolean zhuaniao;
	private boolean qingyise;
	private int niaoshu;
	private Map<String, Integer> playeTotalScoreMap = new HashMap<>();
	private Set<String> xipaiPlayerIds = new HashSet<>();
	private JuResult juResult;

	public MajiangGameValueObject(MajiangGame majiangGame) {
		super(majiangGame);
		panshu = majiangGame.getPanshu();
		renshu = majiangGame.getRenshu();
		dianpao = majiangGame.isDianpao();
		dapao = majiangGame.isDapao();
		quzhongfabai = majiangGame.isQuzhongfabai();
		zhuaniao = majiangGame.isZhuaniao();
		niaoshu = majiangGame.getNiaoshu();
		qingyise = majiangGame.isQingyise();
		playeTotalScoreMap.putAll(majiangGame.getPlayeTotalScoreMap());
		xipaiPlayerIds = new HashSet<>(majiangGame.getXipaiPlayerIds());
		if (majiangGame.getJu() != null) {
			juResult = majiangGame.getJu().getJuResult();
		}
	}

	public boolean isQingyise() {
		return qingyise;
	}

	public void setQingyise(boolean qingyise) {
		this.qingyise = qingyise;
	}

	public int getNiaoshu() {
		return niaoshu;
	}

	public void setNiaoshu(int niaoshu) {
		this.niaoshu = niaoshu;
	}

	public int getPanshu() {
		return panshu;
	}

	public void setPanshu(int panshu) {
		this.panshu = panshu;
	}

	public int getRenshu() {
		return renshu;
	}

	public void setRenshu(int renshu) {
		this.renshu = renshu;
	}

	public boolean isDianpao() {
		return dianpao;
	}

	public void setDianpao(boolean dianpao) {
		this.dianpao = dianpao;
	}

	public boolean isDapao() {
		return dapao;
	}

	public void setDapao(boolean dapao) {
		this.dapao = dapao;
	}

	public boolean isQuzhongfabai() {
		return quzhongfabai;
	}

	public void setQuzhongfabai(boolean quzhongfabai) {
		this.quzhongfabai = quzhongfabai;
	}

	public boolean isZhuaniao() {
		return zhuaniao;
	}

	public void setZhuaniao(boolean zhuaniao) {
		this.zhuaniao = zhuaniao;
	}

	public Map<String, Integer> getPlayeTotalScoreMap() {
		return playeTotalScoreMap;
	}

	public void setPlayeTotalScoreMap(Map<String, Integer> playeTotalScoreMap) {
		this.playeTotalScoreMap = playeTotalScoreMap;
	}

	public JuResult getJuResult() {
		return juResult;
	}

	public void setJuResult(JuResult juResult) {
		this.juResult = juResult;
	}

	public Set<String> getXipaiPlayerIds() {
		return xipaiPlayerIds;
	}

	public void setXipaiPlayerIds(Set<String> xipaiPlayerIds) {
		this.xipaiPlayerIds = xipaiPlayerIds;
	}

}
