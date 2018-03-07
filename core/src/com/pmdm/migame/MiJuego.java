package com.pmdm.migame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;


public class MiJuego extends ApplicationAdapter implements InputProcessor {

    // Strings que apuntan a recursos.
    String recursoMapa   = "mapa.tmx";
    String recursoPlayer = "mosquetero.png";
    String recursoNPC    = "magorojo.png";

    // ============= MAPA , CAMARA Y CAPAS ============= //

    // Objetos para cargar TileMap. Dibujar TileMap. Camara de la vista del juego.
    private TiledMap mapa;
    private OrthogonalTiledMapRenderer mapaRenderer;
    private OrthographicCamera camara;

    // Altura y anchura del mapa en pixeles.
    private int anchoPixelMapa;
    private int altoPixelMapa;
    // Altura y anchura de una celda (tile) del mapa.
    private int altoPixelCelda;
    private int anchoPixelesCelda;

    // Capas de obstáculos y agujeros y Matrices para cargarlas.
    private TiledMapTileLayer capaObstaculos;
    private TiledMapTileLayer capaAgujeros;
    private boolean[][] obstaculo;
    private boolean[][] agujero;
    // Caida en agujero.
    private boolean caida;

    // ============= OBJETOS DE ANIMACION Y TEXTURAS DE SPRITES ============= //

    // Objeto para cargar hojas de sprites (Texture) . Objeto para dibujar imagenes 2D (SpriteBatch).
    private Texture spritesSheet;
    private SpriteBatch spritesBatch;
    // Frame del spriteSheet que se va a mostrar en cada momento.
    private TextureRegion spriteToSHOW;

    // Filas y columnas de las hojas de sprites. Con el tamaño en pixeles se aisla cada frame del muñeco.
    private static final int SPRITESHEET_COLS = 3;
    private static final int SPRITESHEET_ROWS = 4;

    // ============= ANIMACION DEL JUGADOR ============= //

    // Anchura y altura del sprite del jugador en pixeles.
    private int playerSpriteAncho;
    private int playerSpriteAlto;

    // Animación que se muestra en el método render()
    private Animation playerAnimation;
    // Animación direccional del jugador.
    private Animation playerMovesUP;
    private Animation playerMovesRIGHT;
    private Animation playerMovesDOWN;
    private Animation playerMovesLEFT;
    // Posición actual del jugador.
    private float playerPositionX;
    private float playerPositionY;
    // Tiempo en segundos transcurridos desde el inicio de la animación para determinar cual es el frame que se debe representar.
    private float playerAnimationStateTime;

    // ============= ANIMACION DE NPCs ============= //

    // Número de NPCs totales que van a aparecer en el juego.
    private static final int totalNPCs = 100;

    // Anchura y altura del sprite del NPC en pixeles.
    private int npcSpriteAncho;
    private int npcSpriteAlto;

    // Array de animaciones de los NPCs.
    private Animation[] npcAnimation;
    // Animación direccional de los NPCs.
    private Animation npcMovesUP;
    private Animation npcMovesRIGHT;
    private Animation npcMovesDOWN;
    private Animation npcMovesLEFT;
    // Coordenadas iniciales de los NPC.
    private float[] npcInitialPositionX;
    private float[] npcInitialPositionY;
    // Coordenadas finales de los NPC.
    private float[] npcFinalPositionX;
    private float[] npcFinalPositionY;
    // Tiempo en segundos transcurridos desde el inicio de la animación de los NPCs , determina cual es el frame que se debe representar.
    private float stateTimeNPC = 0;

    // ============= MUSICA Y SONIDOS ============= //
    private Music musicBackground;

    private Sound sonidoPasos;
    private Sound sonidoColisionEnemigo;
    private Sound sonidoObstaculo;
    private Sound sonidoCaida;

    // Objeto para mensaje en pantalla.
    private BitmapFont mensajeFinal;

    @Override
    public void create() {

        // Crear cámara con un tamaño fijo para mostrarla siempre igual, posicionar vista en vertice inferior izquierdo.
        camara = new OrthographicCamera(800, 480);
        camara.position.set(camara.viewportWidth / 2f, camara.viewportHeight / 2f, 0);

        // Vinculamos los eventos de entrada a esta clase.
        Gdx.input.setInputProcessor(this);
        camara.update();

        // Cargamos la imagen de los frames del mosquetero en el objeto spritesSheet de la clase Texture.
        spritesSheet = new Texture(Gdx.files.internal(recursoPlayer));

        // Sacamos los frames de spritesSheet en un array de TextureRegion.
        TextureRegion[][] tmp = TextureRegion.split(spritesSheet, spritesSheet.getWidth() / SPRITESHEET_COLS, spritesSheet.getHeight() / SPRITESHEET_ROWS);

        // Creamos las distintas animaciones, teniendo en cuenta que el tiempo de muestra de cada frame
        // será de 150 milisegundos.
        playerMovesUP = new Animation(0.150f, tmp[0]);
        playerMovesRIGHT = new Animation(0.150f, tmp[1]);
        playerMovesDOWN = new Animation(0.150f, tmp[2]);
        playerMovesLEFT = new Animation(0.150f, tmp[3]);
        //En principio se utiliza la animación del jugador arriba como animación por defecto.
        playerAnimation = playerMovesUP;
        // Posición inicial del jugador.
        playerPositionX = playerPositionY = 0;
        //Ponemos a cero el atributo playerAnimationStateTime, que marca el tiempo e ejecución de la animación.
        playerAnimationStateTime = 0f;

        //Creamos el objeto SpriteBatch que nos permitirá representar adecuadamente el sprite en el método render()
        spritesBatch = new SpriteBatch();

        // Cargar mapa.
        mapa = new TmxMapLoader().load(recursoMapa);
        mapaRenderer = new OrthogonalTiledMapRenderer(mapa);

        //Determinamos el alto y ancho del mapa de baldosas. Para ello necesitamos extraer la capa
        //base del mapa y, a partir de ella, determinamos el número de celdas a lo ancho y alto,
        //así como el tamaño de la celda, que multiplicando por el número de celdas a lo alto y
        //ancho, da como resultado el alto y ancho en pixeles del mapa.
        TiledMapTileLayer capa = (TiledMapTileLayer) mapa.getLayers().get(0);
        anchoPixelesCelda = (int) capa.getTileWidth();
        altoPixelCelda = (int) capa.getTileHeight();
        anchoPixelMapa = capa.getWidth() * anchoPixelesCelda;
        altoPixelMapa = capa.getHeight() * altoPixelCelda;

        //Cargamos la capa de los obstáculos, que es la tercera en el TiledMap.
        capaObstaculos = (TiledMapTileLayer) mapa.getLayers().get(2);

        //Cargamos la matriz de los obstáculos del mapa de baldosas.
        int anchoCapa = capaObstaculos.getWidth(), altoCapa = capaObstaculos.getHeight();
        obstaculo = new boolean[altoCapa][anchoCapa];
        for (int x = 0; x < anchoCapa; x++) {
            for (int y = 0; y < altoCapa; y++) {
                obstaculo[x][y] = (capaObstaculos.getCell(x, y) != null);
            }
        }

        //Cargamos la capa de los agujeros, que es la cuarta en el TiledMap.
        capaAgujeros = (TiledMapTileLayer) mapa.getLayers().get(3);

        //Cargamos la matriz de los obstáculos del mapa de baldosas.
        anchoCapa = capaAgujeros.getWidth();
        altoCapa = capaAgujeros.getHeight();
        agujero = new boolean[altoCapa][anchoCapa];
        for (int x = 0; x < anchoCapa; x++) {
            for (int y = 0; y < altoCapa; y++) {
                agujero[x][y] = (capaAgujeros.getCell(x, y) != null);
            }
        }


        //Cargamos en los atributos del ancho y alto del sprite sus valores
        spriteToSHOW = (TextureRegion) playerAnimation.getKeyFrame(playerAnimationStateTime);
        playerSpriteAncho = spriteToSHOW.getRegionHeight();
        playerSpriteAlto = spriteToSHOW.getRegionHeight();

        //Inicializamos el apartado referente a los NPC
        npcAnimation = new Animation[totalNPCs];
        npcInitialPositionX = new float[totalNPCs];
        npcInitialPositionY = new float[totalNPCs];
        npcFinalPositionX = new float[totalNPCs];
        npcFinalPositionY = new float[totalNPCs];

        //Creamos las animaciones posicionales de los NPC
        //Cargamos la imagen de los frames del monstruo en el objeto spritesSheet de la clase Texture.
        spritesSheet = new Texture(Gdx.files.internal(recursoNPC));

        //Sacamos los frames de spritesSheet en un array de TextureRegion.
        tmp = TextureRegion.split(spritesSheet, spritesSheet.getWidth() / SPRITESHEET_COLS, spritesSheet.getHeight() / SPRITESHEET_ROWS);

        // Creamos las distintas animaciones, teniendo en cuenta que el tiempo de muestra de cada frame
        // será de 150 milisegundos.
        npcMovesUP = new Animation(0.150f, tmp[0]);
        npcMovesUP.setPlayMode(Animation.PlayMode.LOOP);
        npcMovesRIGHT = new Animation(0.150f, tmp[1]);
        npcMovesRIGHT.setPlayMode(Animation.PlayMode.LOOP);
        npcMovesDOWN = new Animation(0.150f, tmp[2]);
        npcMovesDOWN.setPlayMode(Animation.PlayMode.LOOP);
        npcMovesLEFT = new Animation(0.150f, tmp[3]);
        npcMovesLEFT.setPlayMode(Animation.PlayMode.LOOP);

        //Cargamos en los atributos del ancho y alto del sprite del monstruo sus valores
        spriteToSHOW = (TextureRegion) npcMovesDOWN.getKeyFrame(stateTimeNPC);
        npcSpriteAncho = spriteToSHOW.getRegionWidth();
        npcSpriteAlto = spriteToSHOW.getRegionHeight();

        //Se inicializan, la animación por defecto y, de forma aleatoria, las posiciones
        //iniciales y finales de los NPC. Para simplificar un poco, los NPC pares, se moveran
        //de forma vertical y los impares de forma horizontal.
        for (int i = 0; i < totalNPCs; i++) {
            npcInitialPositionX[i] = (float) (Math.random() * anchoPixelMapa);
            npcInitialPositionY[i] = (float) (Math.random() * altoPixelMapa);

            if (i % 2 == 0) {
                // NPC par => mover de forma vertical
                npcFinalPositionX[i] = npcInitialPositionX[i];
                npcFinalPositionY[i] = (float) (Math.random() * altoPixelMapa);
                //Determinamos cual de las animaciones verticales se utiliza.
                if (npcInitialPositionY[i] < npcFinalPositionY[i]) {
                    npcAnimation[i] = npcMovesUP;
                } else {
                    npcAnimation[i] = npcMovesDOWN;
                }
            } else {
                // NPC impar => mover de forma horizontal
                npcFinalPositionX[i] = (float) (Math.random() * anchoPixelMapa);
                npcFinalPositionY[i] = npcInitialPositionY[i];
                //Determinamos cual de las animaciones horizontales se utiliza.
                if (npcInitialPositionX[i] < npcFinalPositionX[i]) {
                    npcAnimation[i] = npcMovesRIGHT;
                } else {
                    npcAnimation[i] = npcMovesLEFT;
                }
            }

        }

        // Ponemos a cero el atributo stateTimeNPC, que marca el tiempo e ejecución de la animación
        // de los NPC.
        stateTimeNPC = 0f;

        //Inicializamos la música de fondo del juego y la reproducimos.
        musicBackground = Gdx.audio.newMusic(Gdx.files.internal("dungeon.mp3"));
        musicBackground.setVolume(0.10f);
        musicBackground.play();

        //Inicializamos los atributos de los efectos de sonido.
        sonidoColisionEnemigo = Gdx.audio.newSound(Gdx.files.internal("qubodup-PowerDrain.ogg"));
        sonidoPasos = Gdx.audio.newSound(Gdx.files.internal("Fantozzi-SandR3.ogg"));
        sonidoObstaculo = Gdx.audio.newSound(Gdx.files.internal("wall.ogg"));
        sonidoCaida = Gdx.audio.newSound(Gdx.files.internal("fall.ogg"));

    } // fin create()

    @Override
    public void render() {
        //Ponemos el color del fondo a negro
        Gdx.gl.glClearColor(0, 0, 0, 1);
        //Borramos la pantalla
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //Trasladamos la cámara para que se centre en el mosquetero.
        camara.position.set(playerPositionX, playerPositionY, 0f);
        //Comprobamos que la cámara no se salga de los límites del mapa de baldosas,
        //Verificamos, con el método clamp(), que el valor de la posición x de la cámara
        //esté entre la mitad de la anchura de la vista de la cámara y entre la diferencia entre
        //la anchura del mapa restando la mitad de la anchura de la vista de la cámara,
        camara.position.x = MathUtils.clamp(camara.position.x, camara.viewportWidth / 2f,
                anchoPixelMapa - camara.viewportWidth / 2f);
        //Verificamos, con el método clamp(), que el valor de la posición y de la cámara
        //esté entre la mitad de la altura de la vista de la cámara y entre la diferencia entre
        //la altura del mapa restando la mitad de la altura de la vista de la cámara,
        camara.position.y = MathUtils.clamp(camara.position.y, camara.viewportHeight / 2f,
                altoPixelMapa - camara.viewportHeight / 2f);

        //Actualizamos la cámara del juego
        camara.update();
        //Vinculamos el objeto de dibuja el TiledMap con la cámara del juego
        mapaRenderer.setView(camara);

        //Dibujamos las cuatro primeras capas del TiledMap (no incluye a la de altura)
        int[] capas = {0, 1, 2, 3};
        mapaRenderer.render(capas);

        // extraemos el tiempo de la última actualización del sprite y la acumulamos a playerAnimationStateTime.
        playerAnimationStateTime += Gdx.graphics.getDeltaTime();
        //Extraermos el frame que debe ir asociado a al momento actual.
        spriteToSHOW = (TextureRegion) playerAnimation.getKeyFrame(playerAnimationStateTime);
        // le indicamos al SpriteBatch que se muestre en el sistema de coordenadas
        // específicas de la cámara.
        spritesBatch.setProjectionMatrix(camara.combined);
        //Inicializamos el objeto SpriteBatch
        spritesBatch.begin();
        //Pintamos el objeto Sprite a través del objeto SpriteBatch
        spritesBatch.draw(spriteToSHOW, playerPositionX, playerPositionY);

        //Dibujamos las animaciones de los NPC
        for (int i = 0; i < totalNPCs; i++) {
            actualizaNPC(i, 0.5f);
            spriteToSHOW = (TextureRegion) npcAnimation[i].getKeyFrame(stateTimeNPC);
            spritesBatch.draw(spriteToSHOW, npcInitialPositionX[i], npcInitialPositionY[i]);
        }

        //Finalizamos el objeto SpriteBatch
        spritesBatch.end();

        //Pintamos la quinta capa del mapa de baldosas.
        capas = new int[1];
        capas[0] = 4;
        mapaRenderer.render(capas);

        //Comprobamos si hay o no colisiones entre el jugador y los obstáculos
        detectaColisiones();

        if (caida) {
            // Pinta la pantalla en negro y desactiva los sonidos
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            sonidoPasos.stop();
            sonidoColisionEnemigo.stop();

            // Muestra mensaje de caida
            CharSequence str = "Has caido a un agujero!\nJuego terminado";
            spritesBatch = new SpriteBatch();
            mensajeFinal = new BitmapFont();
            spritesBatch.begin();
            mensajeFinal.draw(spritesBatch, str, Gdx.graphics.getWidth() / 2.0f , Gdx.graphics.getHeight() / 2.0f);
            spritesBatch.end();

            // Espera 3 segundos y cierra el juego
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Gdx.app.exit();
                            System.exit(0);
                        }
                    },
                    3000
            );
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        //Si pulsamos uno de los cursores, se desplaza el sprite
        //de forma adecuada un pixel, y se pone a cero el
        //atributo que marca el tiempo de ejecución de la animación,
        //provocando que la misma se reinicie.
        playerAnimationStateTime = 0;

        //Guardamos la posición anterior del jugador por si al desplazarlo se topa
        //con un obstáculo y podamos volverlo a la posición anterior.
        float jugadorAnteriorX = playerPositionX;
        float jugadorAnteriorY = playerPositionY;

        if (keycode == Input.Keys.LEFT) {
            playerPositionX += -5;
            playerAnimation = playerMovesLEFT;

        }
        if (keycode == Input.Keys.RIGHT) {
            playerPositionX += 5;
            playerAnimation = playerMovesRIGHT;
        }
        if (keycode == Input.Keys.UP) {
            playerPositionY += 5;
            playerAnimation = playerMovesUP;
        }
        if (keycode == Input.Keys.DOWN) {
            playerPositionY += -5;
            playerAnimation = playerMovesDOWN;
        }

        // Detectamos las colisiones con los obstáculos del mapa y si el jugador se sale del mismo.
        // para poner al jugador en su posición anterior
        if ((playerPositionX < 0 || playerPositionY < 0 ||
                playerPositionX > (anchoPixelMapa - playerSpriteAncho) ||
                playerPositionY > (altoPixelMapa - playerSpriteAlto)) ||
                ((obstaculo[(int) ((playerPositionX + playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)]) ||
                        (obstaculo[(int) ((playerPositionX + 3 * playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)]))) {
            playerPositionX = jugadorAnteriorX;
            playerPositionY = jugadorAnteriorY;
            sonidoObstaculo.play(0.10f);

        } else {
            sonidoPasos.play(0.10f);
        }

        // Detectamos las caídas en los agujeros del mapa.
        if ((playerPositionX < 0 || playerPositionY < 0 ||
                playerPositionX > (anchoPixelMapa - playerSpriteAncho) ||
                playerPositionY > (altoPixelMapa - playerSpriteAlto)) ||
                ((agujero[(int) ((playerPositionX + playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)]) ||
                        (agujero[(int) ((playerPositionX + 3 * playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)]))) {

            caida = true;
            musicBackground.pause();
            sonidoCaida.play(0.10f);
        } else {
            sonidoPasos.play(0.10f);
        }

        //Si pulsamos la tecla del número 1, se alterna la visibilidad de la primera capa
        //del mapa de baldosas.
        if (keycode == Input.Keys.NUM_1)
            mapa.getLayers().get(0).setVisible(!mapa.getLayers().get(0).isVisible());
        //Si pulsamos la tecla del número 2, se alterna la visibilidad de la segunda capa
        //del mapa de baldosas.
        if (keycode == Input.Keys.NUM_2)
            mapa.getLayers().get(1).setVisible(!mapa.getLayers().get(1).isVisible());

        if ((obstaculo[(int) ((playerPositionX + playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)])
                || (obstaculo[(int) ((playerPositionX + 3 * playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)])) {
            playerPositionX = jugadorAnteriorX;
            playerPositionY = jugadorAnteriorY;
        }


        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Vector en tres dimensiones que recoge las coordenadas donde se ha hecho click
        // o toque de la pantalla.
        Vector3 clickCoordinates = new Vector3(screenX, screenY, 0);
        // Transformamos las coordenadas del vector a coordenadas de nuestra cámara.
        Vector3 posicion = camara.unproject(clickCoordinates);

        //Se pone a cero el atributo que marca el tiempo de ejecución de la animación,
        //provocando que la misma se reinicie.
        playerAnimationStateTime = 0;

        //Guardamos la posición anterior del jugador por si al desplazarlo se topa
        //con un obstáculo y podamos volverlo a la posición anterior.
        float jugadorAnteriorX = playerPositionX;
        float jugadorAnteriorY = playerPositionY;

        //Si se ha pulsado por encima de la animación, se sube esta 5 píxeles y se reproduce la
        //animación del jugador desplazándose hacia arriba.
        if ((playerPositionY + 48) < posicion.y) {
            playerPositionY += 5;
            playerAnimation = playerMovesUP;
            //Si se ha pulsado por debajo de la animación, se baja esta 5 píxeles y se reproduce
            //la animación del jugador desplazándose hacia abajo.
        } else if ((playerPositionY) > posicion.y) {
            playerPositionY -= 5;
            playerAnimation = playerMovesDOWN;
        }
        //Si se ha pulsado mas de 24 a la derecha de la animación, se mueve esta 5 píxeles a la derecha y
        //se reproduce la animación del jugador desplazándose hacia la derecha.
        if ((playerPositionX + 24) < posicion.x) {
            playerPositionX += 5;
            playerAnimation = playerMovesRIGHT;
            //Si se ha pulsado más de 24 a la izquierda de la animación, se mueve esta 5 píxeles a la
            // izquierda y se reproduce la animación del jugador desplazándose hacia la izquierda.
        } else if ((playerPositionX - 24) > posicion.x) {
            playerPositionX -= 5;
            playerAnimation = playerMovesLEFT;
        }

        // Detectamos las colisiones con los obstáculos del mapa y si el jugador se sale del mismo.
        // para poner al jugador en su posición anterior
        if ((playerPositionX < 0 || playerPositionY < 0 ||
                playerPositionX > (anchoPixelMapa - playerSpriteAncho) ||
                playerPositionY > (altoPixelMapa - playerSpriteAlto)) ||
                ((obstaculo[(int) ((playerPositionX + playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)]) ||
                        (obstaculo[(int) ((playerPositionX + 3 * playerSpriteAncho / 4) / anchoPixelesCelda)][((int) (playerPositionY) / altoPixelCelda)]))) {
            playerPositionX = jugadorAnteriorX;
            playerPositionY = jugadorAnteriorY;
            sonidoObstaculo.play(0.10f);

        } else {
            sonidoPasos.play(0.10f);
        }

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    //Método que permite cambiar las coordenadas del NPC en la posición "i",
    //dada una variación "delta" en ambas coordenadas.
    private void actualizaNPC(int i, float delta) {
        if (npcFinalPositionY[i] > npcInitialPositionY[i]) {
            npcInitialPositionY[i] += delta;
            npcAnimation[i] = npcMovesUP;
        }
        if (npcFinalPositionY[i] < npcInitialPositionY[i]) {
            npcInitialPositionY[i] -= delta;
            npcAnimation[i] = npcMovesDOWN;
        }
        if (npcFinalPositionX[i] > npcInitialPositionX[i]) {
            npcInitialPositionX[i] += delta;
            npcAnimation[i] = npcMovesRIGHT;
        }
        if (npcFinalPositionX[i] < npcInitialPositionX[i]) {
            npcInitialPositionX[i] -= delta;
            npcAnimation[i] = npcMovesLEFT;
        }
    }

    private void detectaColisiones() {
        // Crear un "hitbox" para cada sprite con la clase Rectangle y las medidas del Sprite. Después se comprueba si se solapan.
        Rectangle playerHitbox = new Rectangle(playerPositionX, playerPositionY, playerSpriteAncho, playerSpriteAlto);
        Rectangle npcHitbox;

        // Para cada NPC del array se genera 1 hitbox y se comprueba si se solapa con el del Jugador.
        for (int i = 0; i < totalNPCs; i++) {

            npcHitbox = new Rectangle(npcInitialPositionX[i], npcInitialPositionY[i], npcSpriteAncho, npcSpriteAlto);

            if (playerHitbox.overlaps(npcHitbox)) {
                // Hacer lo que haya que hacer , ej: reproducir un efecto de sonido, una animación del jugador alternativa y, posiblemente, que este muera y se acabe la partida actual.
                // En este caso lo único que se hace es mostrar un mensaje en la consola de texto.
                System.out.println("Hay colisión !!");
                sonidoColisionEnemigo.play(0.10f);
            }
        }
    }

    // Liberar recursos.
    public void dispose() {
        mapa.dispose();
        mapaRenderer.dispose();
        spritesSheet.dispose();
        spritesBatch.dispose();
        musicBackground.dispose();
        sonidoObstaculo.dispose();
        sonidoPasos.dispose();
        sonidoColisionEnemigo.dispose();
    }

}