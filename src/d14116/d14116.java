package d14116;

import robocode.*;
import java.awt.Color;
import static robocode.util.Utils.*;

public class d14116 extends Robot
{

    /* 変数宣言 */
    boolean ahead_flag = true;
    boolean target = false;
    double amountY;
    double amountX;
    int scope = 2;
    int cnt = 0;

    /* 移動するための自作関数 */
	public void go() {
        if(ahead_flag) ahead(100 + Math.round(Math.random() * 100));
        else back(100 + Math.round(Math.random() * 100));
    }

    /* 基本の動作 */
	public void run() {
        /* 色 */
        setBodyColor(Color.black);
		setGunColor(Color.blue);
		setRadarColor(Color.black);
		setBulletColor(Color.black);
        /* バトルフィールドの高さを取得 */
        amountY = getBattleFieldHeight();
        /* バトルフィールドの幅を取得 */
        amountX = getBattleFieldWidth();
        /* 砲塔を本体から独立 */
        setAdjustGunForRobotTurn(true);
        /* レーダーを砲塔から独立 */
        setAdjustRadarForGunTurn(true);
        /* 繰り返し処理 */
		while(true) {
            /* 敵探索 */
            target = false;
            scan();
            if(target) continue;
            turnRadarRight(90);
            if(target) continue;
            turnRadarLeft(180);
            if(target) continue;
            turnRadarLeft(180);
		}
    }

    /* 敵を発見したときの動作 */
	public void onScannedRobot(ScannedRobotEvent e) {
        /* 敵発見 */
        target = true;
        /* 敵の位置の絶対角度 */
        double absoluteBearing = getHeading() + e.getBearing();
        /* 砲塔からの相対角度 */
		double bearingFromGun = 
            normalRelativeAngleDegrees(absoluteBearing - getGunHeading());
        /* 弾のパワー */
        double power;
        double diagonal = Math.sqrt(Math.pow(getBattleFieldHeight(), 2) + 
            Math.pow(getBattleFieldWidth(), 2));
        if(e.getDistance() < 100) power=3;
        else power = (3 - (e.getDistance()  - 100) * 
            2.9 / (diagonal - 100));
        power = Math.min(power, Math.max((getEnergy() / 10), 0.1));
        /* 予測 */
        double dX = 
            e.getDistance() * Math.cos(absoluteBearing * Math.PI / 180);
        double dY = 
            e.getDistance() * Math.sin(absoluteBearing * Math.PI / 180);
        double vX = 
            e.getVelocity() * Math.cos(e.getHeading() * Math.PI / 180);
        double vY = 
            e.getVelocity() * Math.sin(e.getHeading() * Math.PI / 180);
        double bulletSpeed = 20 - power * 3;
        /* 解の公式で、発射から到達の時間't'を求める */
        double a =  vX * vX + vY * vY - bulletSpeed * bulletSpeed;
        double b = (dX * vX + dY * vY) * 2;
        double c = (dX * dX + dY * dY);
        double t = (-b + Math.sqrt(b * b -4 * a * c)) / (2 * a);
        if(t < 0)
            t = (-b - Math.sqrt(b * b -4 * a * c)) / (2 * a);
        double nextAbsBearing = 0;
        if((dX + vX * t) > 0) nextAbsBearing = 
            Math.atan((dY + vY * t) / (dX + vX * t)) * 180 / Math.PI;
        else nextAbsBearing = 
            180 + Math.atan((dY + vY * t) / (dX + vX * t)) * 
            180 / Math.PI;
		double nextBearingFromGun = 
            normalRelativeAngleDegrees
            (nextAbsBearing - getGunHeading());
        /* 砲塔を傾ける時間を考慮する。砲塔の回転速度は'20' */
        t += Math.abs(nextBearingFromGun) / 20;
        if((dX + vX * t) > 0) nextAbsBearing = 
            Math.atan((dY + vY * t) / (dX + vX * t)) * 180 / Math.PI;
        else nextAbsBearing = 
            180 + Math.atan((dY + vY * t) / (dX + vX * t)) * 
            180 / Math.PI;
		nextBearingFromGun = 
            normalRelativeAngleDegrees
            (nextAbsBearing - getGunHeading());
        /* 狙い撃ち */
        switch(scope) {
            case 2:
                turnGunRight(nextBearingFromGun);
                break;
            case 1:
                turnGunRight(bearingFromGun + 
                    Math.random() * (nextBearingFromGun - bearingFromGun));
                break;
            default:
                turnGunRight(bearingFromGun);
                break;
        }
        fire(power);
        /* 砲塔とレーダーのずれを補正 */
        double diff = getGunHeading() - getRadarHeading();
        turnRadarRight(normalRelativeAngleDegrees(diff));
        /* 敵の方向に対して90度傾けたあと移動する */
        if(e.getBearing() < 0) 
            turnRight(90 + e.getBearing());
        else 
            turnLeft(90 - e.getBearing());
        go();
	}

    /* 攻撃を当てたときの動作 */
    public void onBulletHit(BulletHitEvent e) {
        cnt = 0;
    }

    /* 攻撃をはずしたときの動作 */
    public void onBulletMissed(BulletMissedEvent e) {
        cnt++;
        if(cnt == 5) {
            cnt = 0;
            if(scope > 0) scope--;
            else scope = 2;
        }
    }

    /* 敵の攻撃に当たったときの動作 */
	public void onHitByBullet(HitByBulletEvent e) {
        if(e.getBearing() < 0) turnRight(90 + e.getBearing());
        else turnLeft(90 - e.getBearing());
        go();
	}

    /* 敵に当たったときの動作 */
	public void onHitRobot(HitRobotEvent e) {
        /* 敵の位置の絶対角度 */
        double absoluteBearing = getHeading() + e.getBearing();
        /* 砲塔からの相対角度 */
		double bearingFromGun = 
            normalRelativeAngleDegrees(absoluteBearing - getGunHeading());
        /* 弾のパワー */
        double power = Math.min(3, Math.max((getEnergy() / 10), 0.1));
        /* 敵に攻撃 */
        turnGunRight(bearingFromGun);
        fire(power);
        /* 砲塔とレーダーのずれを補正 */
        double diff = getGunHeading() - getRadarHeading();
        turnRadarRight(normalRelativeAngleDegrees(diff));
    }

    /* 壁に当たったときの動作 */
	public void onHitWall(HitWallEvent e) {
        if(ahead_flag) ahead_flag = false;
        else ahead_flag = true;
        if(((getY() < 200) || (amountY - 200 < getY())) && 
                ((getX() < 200) || (amountX - 200 < getX()))) {
            if(Math.abs(e.getBearing()) < 90) turnRight(e.getBearing());
            else {
                double turn;
                if(e.getBearing() < 0) turn = -180 - e.getBearing();
                else turn = 180 - e.getBearing();
                turnLeft(turn);
            }
            go();
        }
    }

    /* 勝ったときの動作 */
    public void onWin(WinEvent e) {
        /* よろこぶ */
        ahead(0);
		turnRight(0);
		turnGunRight(0);
		turnRadarRight(0);
		turnRight(10000);
	}

}
