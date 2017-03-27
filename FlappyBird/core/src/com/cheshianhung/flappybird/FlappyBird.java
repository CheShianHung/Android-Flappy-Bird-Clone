package com.cheshianhung.flappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;

import java.util.Random;

public class FlappyBird extends ApplicationAdapter {

	private SpriteBatch batch;
	private Animation<Texture> birdAnimation;
	private Texture[] bird;
	private Texture background;
	private Texture topTube;
	private Texture bottomTube;
	private Texture gameOverImg;
	private BitmapFont scoreFont;
	private BitmapFont bestScoreFont;
	private BitmapFont instructText;
	private Circle birdCircle;
	private Rectangle[][] tubeRectangle;
	private int tubeNumber = 4;
	private int score;
	private int bestScore;
	private int scoreTube;
	private int gameState;
	private float elapsedTime;
	private float yForce;
	private float jumpForce;
	private float gravity;
	private float currentYPos; //current y position for the bird
	private float[] pipeXPos = new float[tubeNumber]; //current x position for each pipe
	private float[] pipeYPos = new float[tubeNumber]; // current y position for each pipe
	private float pipeVerticalDistance; //distance between top tube and bottom tube
	private float pipeHorizontalDistance; //distance between the first pipe and the second pipe
	private float minPipePosition;
	private float maxPipePosition;
	private float pipeVelocity;
	private float gameOverImgPos;
	private float gameOverImgStartY;
	private float gameOverImgEndY;
	private float gameOverImgFallingSpeed;
	private float textTimer;
	private boolean textSwitch;
	private boolean saveBest;
	private Random rand;

	@Override
	public void create () {

		//retrieve best score
		Preferences preferences = Gdx.app.getPreferences("flappyBird");
		bestScore = preferences.getInteger("bestScore", 0);

		//set batch, texture, shapes
		batch = new SpriteBatch();
		background = new Texture("bg.png");
		bird = new Texture[2];
		bird[0] = new Texture("bird_0.png");
		bird[1] = new Texture("bird_1.png");
		topTube = new Texture("toptube.png");
		bottomTube = new Texture("bottomtube.png");
		birdCircle = new Circle();
		tubeRectangle = new Rectangle[tubeNumber][2];
		for(int i = 0; i < tubeNumber; i++){
			tubeRectangle[i][0] = new Rectangle();
			tubeRectangle[i][1] = new Rectangle();
		}

		//set animation
		birdAnimation = new Animation<Texture>(1f/2f, bird);

		//set jumping data
		yForce = 0f;
		jumpForce = 17f;
		gravity = -0.7f;
		currentYPos = Gdx.graphics.getHeight() / 2 - bird[0].getHeight() / 2;

		//set pipes data
		pipeVerticalDistance = bird[0].getHeight() * 3.9f;
		pipeHorizontalDistance = Gdx.graphics.getWidth() / 3 * 2;
		minPipePosition = Gdx.graphics.getHeight() / 8;
		maxPipePosition = Gdx.graphics.getHeight() / 8 * 7 - pipeVerticalDistance;
		pipeVelocity = 5f;

		//timer
		elapsedTime = 0f;

		//initialize pipe position
		rand = new Random();
		for(int i = 0; i < tubeNumber; i++) {
			pipeXPos[i] = Gdx.graphics.getWidth() * 1.5f + i * pipeHorizontalDistance;
			pipeYPos[i] = rand.nextFloat() * (maxPipePosition - minPipePosition) + minPipePosition;
		}

		//set score
		scoreFont = new BitmapFont();
		scoreFont.setColor(Color.WHITE);
		scoreFont.getData().setScale(5);
		bestScoreFont = new BitmapFont();
		bestScoreFont.setColor(Color.WHITE);
		bestScoreFont.getData().setScale(3);

		//set instruction text
		instructText = new BitmapFont();
		instructText.setColor(Color.WHITE);
		instructText.getData().setScale(5);

		//set game over image
		gameOverImg = new Texture("gameover.png");
		gameOverImgStartY = Gdx.graphics.getHeight();
		gameOverImgEndY = Gdx.graphics.getHeight() / 3 * 2 - gameOverImg.getHeight() / 2;
		gameOverImgPos = gameOverImgStartY;
		gameOverImgFallingSpeed = 7f;

		//initialize other variables
		gameState = 0;
		score = 0;
		scoreTube = 0;
		textTimer = 0;
		saveBest = false;

	}

	@Override
	public void render () {

		//if the player taps the screen
		if(Gdx.input.justTouched()){
			//if the game has not started yet
			if(gameState == 0) {
				gameState = 1; //start the game
				yForce = jumpForce;
			}
			//during the game play
			else if(gameState == 1) {
				//update the jump force
				yForce = jumpForce;
			}
			//if game over
			else if(gameState == 2) {
				gameState = 0;

				//initialize data
				score = 0;
				scoreTube = 0;
				textTimer = 0;
				textSwitch = false;
				saveBest = false;
				gameOverImgPos = gameOverImgStartY;
				currentYPos = Gdx.graphics.getHeight() / 2 - bird[0].getHeight() / 2;
				for(int i = 0; i < tubeNumber; i++) {
					pipeXPos[i] = Gdx.graphics.getWidth() * 1.5f + i * pipeHorizontalDistance;
					pipeYPos[i] = rand.nextFloat() * (maxPipePosition - minPipePosition) + minPipePosition;
				}
			}

		}

		//if the game has not started yet
		if(gameState == 0){
			batch.begin();
			batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.draw(bird[0], Gdx.graphics.getWidth() / 3, currentYPos);

			showScore();
			showInstuctText();

			batch.end();
		}

		//during the game play
		else if (gameState == 1) {
			//update jump force and time
			yForce += gravity;
			currentYPos += yForce;
			elapsedTime += Gdx.graphics.getDeltaTime();

			batch.begin();
			batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.draw(birdAnimation.getKeyFrame(elapsedTime, true), Gdx.graphics.getWidth() / 3, currentYPos);

			//update pipe
			for(int i = 0; i < tubeNumber; i++){

				//if the pipe has passed through the screen
				if (pipeXPos[i] < - topTube.getWidth()) {
					pipeXPos[i] = tubeNumber * pipeHorizontalDistance - topTube.getWidth();
					pipeYPos[i] = rand.nextFloat() * (maxPipePosition - minPipePosition) + minPipePosition;
				}

				batch.draw(topTube, pipeXPos[i], pipeYPos[i] + pipeVerticalDistance);
				batch.draw(bottomTube, pipeXPos[i], pipeYPos[i] - bottomTube.getHeight());
				pipeXPos[i] -= pipeVelocity;
			}

			showScore();

			batch.end();
		}

		//collision detection
		birdCircle.set(Gdx.graphics.getWidth() / 3 + bird[0].getWidth() / 2, currentYPos + bird[0].getHeight() / 2, bird[0].getHeight() / 2);
		for(int i = 0; i < tubeNumber; i++){
			tubeRectangle[i][0].set(pipeXPos[i], pipeYPos[i] + pipeVerticalDistance, topTube.getWidth(), topTube.getHeight());
			tubeRectangle[i][1].set(pipeXPos[i], pipeYPos[i] - bottomTube.getHeight(), bottomTube.getWidth(), bottomTube.getHeight());
			if(Intersector.overlaps(birdCircle, tubeRectangle[i][0]) || Intersector.overlaps(birdCircle, tubeRectangle[i][1])){
				gameState = 2;
			}

			//update score
			if(scoreTube % tubeNumber == i && pipeXPos[i] < Gdx.graphics.getWidth() / 3 - topTube.getWidth()){
				score++;
				scoreTube++;
			}
		}

		//if the bird is out of border or hit cube
		if(currentYPos < 0 || currentYPos > Gdx.graphics.getHeight() - bird[0].getHeight()){
			gameState = 2;
		}

		//game over
		if(gameState == 2){
			batch.begin();
			batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.draw(birdAnimation.getKeyFrame(elapsedTime, true), Gdx.graphics.getWidth() / 3, currentYPos);

			for(int i = 0; i < tubeNumber; i++){
				batch.draw(topTube, pipeXPos[i], pipeYPos[i] + pipeVerticalDistance);
				batch.draw(bottomTube, pipeXPos[i], pipeYPos[i] - bottomTube.getHeight());
			}

			showScore();

			batch.draw(gameOverImg, Gdx.graphics.getWidth() / 2 - gameOverImg.getWidth() / 2, gameOverImgPos);
			if(gameOverImgPos > gameOverImgEndY){
				gameOverImgPos -= gameOverImgFallingSpeed;
			}
			else{
				showInstuctText();
			}

			//save best
			if(score >= bestScore && !saveBest){
				Preferences preferences = Gdx.app.getPreferences("flappyBird");
				preferences.putInteger("bestScore", score);
				preferences.flush();
				saveBest = true;
			}

			batch.end();
		}

	}
	
	@Override
	public void dispose () {
		batch.dispose();
		bird[0].dispose();
		bird[1].dispose();
		background.dispose();
		topTube.dispose();
		bottomTube.dispose();
	}

	private void showScore(){
		if(score > bestScore){
			bestScore = score;
		}
		scoreFont.draw(batch, "Score: " + String.valueOf(score), 70, Gdx.graphics.getHeight() - 70);
		bestScoreFont.draw(batch, "Best Score: " + String.valueOf(bestScore), 70, Gdx.graphics.getHeight() - 150);
	}

	private void showInstuctText(){
		if(gameState == 0 && textSwitch) {
			instructText.draw(batch, "Tap Screen to Start the Game", 50, Gdx.graphics.getHeight() / 4 - 100);
		}
		else if(gameState == 2 && textSwitch) {
			instructText.draw(batch, "Tap Screen to Restart", 50, Gdx.graphics.getHeight() / 4 - 100);
		}

		textTimer += Gdx.graphics.getDeltaTime();
		if(textTimer > 0.7f) {
			textSwitch = !textSwitch;
			textTimer = 0f;
		}
	}
}
