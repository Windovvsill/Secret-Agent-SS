package com.afternooncoffeesoftware.sass;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by cole on 2014-10-10.
 */
public class Level implements com.badlogic.gdx.Screen {
    BitmapFont debugFont;
    Player player;
    OrthographicCamera camera;
    Input input;
    SpriteBatch batch;
    NPC guard;
    NPC guard2;
    NPC entranceGuard1;

    Object paper, ball;

    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Minecraftia-Regular.ttf"));
    FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
    FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
    //in-game font

    BitmapFont font = new BitmapFont();
    BitmapFont helperFont = new BitmapFont();

    Array<Bullet> bullets;

    int x = 0, y = 0;
    Rectangle mouseBox;

    public static TextureRegion currentFrame;
    boolean last = false;

    public int globalOffset = 0;

    //test revert successfull

    static float stateTime;

    public Level() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        Art.load();
        Sound.load();
        debugFont = new BitmapFont();
        debugFont.setColor(1.0f, 0.0f, 0.0f, 1.0f);

        // x was 800/3*2 = 533.333
        player = new Player(500, 50);

        guard = new NPC(Art.nekkidImg);
        guard2 = new NPC(Art.nekkidImg);
        entranceGuard1 = new NPC(Art.nekkidImg);

        input = new Input(this);
        stateTime = 0f;

        parameter.size = 32;
        parameter2.size = 16;
        font = generator.generateFont(parameter);
        helperFont = generator.generateFont(parameter2);

        paper = new Object("Paper", Art.paperImg);
        ball = new Object("Ball", Art.ballImg);


        mouseBox = new Rectangle(0, 0, 2, 2);

        bullets = new Array<Bullet>();
    }

    public void render(float delta) {
        input.level(player);

        //set mouse x and y
        x = Gdx.input.getX();
        y = Gdx.input.getY();
        mouseBox.setPosition(x - (mouseBox.getWidth() / 2), 480 - y - (mouseBox.getHeight() / 2));

        //debugging string
        CharSequence str = player.toString();
        CharSequence str2 = "Box found!";

        //set up a white canvas
        Gdx.gl.glClearColor(0.8f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Art.batch.setProjectionMatrix(camera.combined);
        Art.levelBgBox.x = globalOffset + 200;

        //render scene
        batch.begin();
        Art.levelBgSprite.draw(batch);
        Art.levelBgSprite.setPosition(Art.levelBgBox.x, Art.levelBgBox.y);

        //debugging
        if (Intersector.overlaps(mouseBox, player.box)) debugFont.draw(batch, str2, 10, 400);

        if (input.fire) {
            fireBullet();
        }

        font.draw(batch, "GET TO THE MEETING", 200, 440);
        font.draw(batch, "Items in inventory: " + player.inventory.size(), 200, 400);

        //initialize objects and positions
        guard.draw(batch);
        guard2.draw(batch);
        entranceGuard1.draw(batch);

        if (!player.inventory.contains(paper))
            paper.draw(batch);

        player.draw(batch);

        if (!player.inventory.contains(ball))
            ball.draw(batch);

        guard.setPosition(globalOffset + 400, 90);
        guard2.setPosition(globalOffset + 700, 90);
        entranceGuard1.setPosition(globalOffset + 1000, 90);

        paper.setPosition(globalOffset + 780, 150);
        ball.setPosition(globalOffset + 200, 80);
        player.setPosition(player.box.x, player.box.y);

        playerAnimate();
        npcEvents();
        objectEvents();
        iterBullets();

        batch.end();
    }

    public void playerAnimate(){
        //animation variables
        stateTime += Gdx.graphics.getDeltaTime();
        currentFrame = Art.walkAnimation.getKeyFrame(stateTime, true);

        if (input.walkRight) {
            if (currentFrame.isFlipX()) currentFrame.flip(true, false);

            player.sprite.setRegion(currentFrame);
            Sound.walk.play();
            last = false;
        }
        if (input.walkLeft) {
            if (!currentFrame.isFlipX()) currentFrame.flip(true, false);
            player.sprite.setRegion(currentFrame);
            Sound.walk.play();
            last = true;
        }
        if (!input.walkLeft && !input.walkRight) {
            if (!last) {
                if (Art.playerRegIdle.isFlipX()) Art.playerRegIdle.flip(true, false);
                player.sprite.setRegion(Art.playerRegIdle);
            }
            if (last) {
                if (!Art.playerRegIdle.isFlipX()) Art.playerRegIdle.flip(true, false);
                player.sprite.setRegion(Art.playerRegIdle);
            }
        }
    }

    public void fireBullet() {
        Bullet bullet;
        if (input.faceRight) {
            bullet = new Bullet(true);
            bullet.setPosition(player.box.x + 30, 100);
        } else {
            bullet = new Bullet(false);
            bullet.setPosition(player.box.x, 100);
        }
        bullets.add(bullet);
    }

    public void iterBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.draw(batch);
            int speed = 15;
            if (b.facingRight())
                b.box.x += speed;
            else
                b.box.x -= speed;
            if (!b.visible()) it.remove();
        }
    }


    public void npcEvents(){
        if (guard.talkative) {
            if (Intersector.overlaps(player.box, guard.box)) {
                ScreenManager.getInstance().showDialog(1, guard);
            }
        }
        //restarts conversation with not active guard with SPACE BAR
        else {
            if (Intersector.overlaps(player.box, guard.box) && Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE))
                ScreenManager.getInstance().showDialog(3, guard);
        }
        if (guard2.talkative) {
            if (Intersector.overlaps(player.box, guard2.box)) {
                ScreenManager.getInstance().showDialog(3, guard2);
            }
        }
        //talks to an entrance guard only if space is pressed
        if (!entranceGuard1.talkative) {
            if (Intersector.overlaps(player.box, entranceGuard1.box)) {
                if (Intersector.overlaps(player.box, entranceGuard1.box) && Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE))
                    ScreenManager.getInstance().showDialog(4, entranceGuard1);
            }
            }
        }


    public void objectEvents(){
        intersectAdd(paper);
        intersectAdd(ball);
    }

    public void intersectAdd(Object obj) {
        if (Intersector.overlaps(player.box, obj.box)) {
            if (!player.inventory.contains(obj)) {
                helperFont.draw(batch, "<E>", player.getCenterX(), player.getCenterY() + 140);
                if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {
                    if (player.inventory.size() <= 6) {
                        player.inventory.add(obj);
                        obj.box.setPosition(0, 0);
                    }
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        debugFont.dispose();
        Sound.select.dispose();
        Sound.menuMusic.dispose();
        generator.dispose(); // don't forget to dispose to avoid memory leaks!
    }
}
