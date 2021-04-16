package jc;
import robocode.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import static robocode.util.Utils.normalRelativeAngle;
import java.awt.*;

public class RobocodeRobot extends AdvancedRobot {
	boolean forward; // Whether the robot is moving forward or backwards.
	boolean nearWall; // True when the robot is near the wall.
	int numOfTurns; // The number of turns/ticks deciding how often the robot strafes.
	double energyChange; // This stores the change in energy for the currently scanned enemy robot.
	double previousEnergy = 100; // The previous energy of the robot we are scanning. Allows us to calculate the change in energy.
	double bulletPower; // The power of the bullet that the robot will use.

	public void run() {
		// Allow each part of the robot to move freely from the others.
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		// Move the robot forward by 100,000 pixels.
		setAhead(100000);
		forward = true;
		
		// Turn the radar right by 360 degrees to try and find an enemy.
		setTurnRadarRight(360);
		
		// Set the robots colours
		setGunColor(new Color(00, 00, 00)); // Black
		setRadarColor(new Color(255, 00, 00)); // Red
		setBodyColor(new Color(255, 00, 00)); // Red
		setBulletColor(new Color(220, 00, 255)); // Pink
		
		// Wall avoidance
		// Check if the robot is closer than 60px from the wall.
		if (getX() <= 60 || getY() <= 60 || getBattleFieldWidth() - getX() <= 60 || getBattleFieldHeight() - getY() <= 60) {
			nearWall = true;
		} else {
			nearWall = false;
		}

		while (true) {
			// More wall avoidance
			// If the robot is away from all walls, set nearWall to false.
			if (getX() > 60 && getY() > 60 && getBattleFieldWidth() - getX() > 60 && getBattleFieldHeight() - getY() > 60 && nearWall == true) {
				nearWall = false;
			}
			
			// If the robot is near a wall and nearWall is set to false, set it to true and move in the opposite direction.
			if (getX() <= 60 || getY() <= 60 || getBattleFieldWidth() - getX() <= 60 || getBattleFieldHeight() - getY() <= 60 ) {
				if ( nearWall == false){
					changeDirection();
					nearWall = true;
				}
			}
 
			// Strafing
			// Every 40/30 turns/ticks of a round, the robot will change direction and therefore strafe.
			if (forward == true) {
				numOfTurns = 40;
			} else {
				numOfTurns = 30;
			}
			
			if (getTime() % numOfTurns == 0) {
				changeDirection();
			}

			// If the radar stops turning, turn the radar right by 360 degrees to try and find a new enemy.
			if (getRadarTurnRemaining() == 0.0){
				setTurnRadarRight(360);
			}
			
			// Executes all of the set commands. e.g. setAhead.
			execute();
		}
	}
 	
	public void onScannedRobot(ScannedRobotEvent e) {	
		// Work out the fire power based on distance from enemy.
		if (e.getDistance() > 350) {
			bulletPower = 1;
		} else if (e.getDistance() > 100) {
			bulletPower = 2;
		} else {
			bulletPower = 3;
		}

		//  Linear predictive targeting
		double bulletVelocity = 20 - 3 * bulletPower; // The speed of the robots bullet.
		double enemyAbsoluteBearingRadians = getHeadingRadians() + e.getBearingRadians(); // Angle between north and enemy observed from the robot in radians.
		double enemyLateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearingRadians); // The enemy robots velocity perpendicular to the robot.
		
		setTurnGunRightRadians(normalRelativeAngle((enemyAbsoluteBearingRadians - getGunHeadingRadians()) + (enemyLateralVelocity / bulletVelocity))); // Turns the gun ahead of the enemy, to the point where they will be when the bullet reaches them.

		double enemyAbsoluteBearing = getHeading() + e.getBearing(); // Angle between north and enemy observed from the robot in degrees.
		double enemyDegreesFromRadar = normalRelativeAngleDegrees(enemyAbsoluteBearing - getRadarHeading()); // Angel between enemy robots absolute bearing and the heading of the radar.
		
		setTurnRadarRight(enemyDegreesFromRadar); // Keeps the radar focused on the enemy.
	
		// Fire the gun if gun heat is 0 so we won't get fired into disability.
		if (getGunHeat() == 0 && (getEnergy() - bulletPower) >= 0.1) {
			fire(bulletPower);
		}

		// Circling
		// Circle around the enemy robot and move closer.
		if (forward == true){
			setTurnRight(normalRelativeAngleDegrees(e.getBearing() + 80));
		} else {
			setTurnRight(normalRelativeAngleDegrees(e.getBearing() + 100));
		}
		
 		// Evading
		// If close to enemy
		if (e.getDistance() < 100) { 
			// And if enemy is infront of the robot then move back
			if (e.getBearing() > -90 && e.getBearing() < 90) {
				setBack(120);
			// Else if behind the robot, move forward
			} else {
				setAhead(120);
			}
		}		
		
		// Dodgeing bullets
		// If the enemy robots energy change is small, change direction to avoid bullet.
		energyChange = previousEnergy - e.getEnergy();
		if (energyChange > 0 && energyChange <= 3) {
			changeDirection();
		}
		
		// Calls the OnScannedRobot method if a robot has been seen.
		if (enemyDegreesFromRadar == 0) {
			scan();
		}
		
		// Store the enemies current energy in previous energy.
		previousEnergy = e.getEnergy();
	}		
 
	// If the avoiding walls code doesn't work and the robot hits a wall, reverse the direction of the robot.
	public void onHitWall(HitWallEvent e) {
		changeDirection();
	}

	public void onHitRobot(HitRobotEvent e) {
		// If we hit another robot, then reverse.
		if (e.isMyFault()) {
			changeDirection();
		}
	}
	
	// Reverses the direction of the robot.
	public void changeDirection() {
		if (forward == true) {
			setBack(100000);
			forward = false;
		} else {
			setAhead(100000);
			forward = true;
		}
	}
	
	// If the robot has wn the game, then stop and do a victory dance.
	public void onWin(WinEvent e) {
		for (int i = 0; i < 60; i++) {
			setAhead(0);
			turnRight(30);
			turnLeft(30);
			fire(3);
		}
	}
}