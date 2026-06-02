package mygame;
 
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
 
/**
 * =========================================================
 *  MAIN — Punto de entrada y controlador del menú principal
 * =========================================================
 *  Clase principal que arranca el juego (extiende SimpleApplication de jMonkeyEngine).
 *
 *  Responsabilidades:
 *    1. Animar la pantalla de presentación del Pip-Boy (58 frames de PNG).
 *    2. Mostrar el menú de selección de modo una vez terminada la animación.
 *    3. Instanciar el modo elegido, pasarle los recursos compartidos e iniciarlo.
 *    4. Gestionar el regreso al menú (ESC) y el reinicio (R).
 *    5. Reproducir la música del menú (musica5.wav), pausarla al entrar a un modo
 *       y reanudarla al volver.
 *
 *  Modos disponibles:
 *    1 → ModoClasico       4 → ModoZombies
 *    2 → ModoAbanico       5 → ModoInfierno (2 jugadores)
 *    3 → ModoSupervivencia
 * */
public class Main extends SimpleApplication {
 
    // --- Variables de Animación ---
    // Controlan la animación de entrada del Pip-Boy al abrir el juego.
    // Se cargan 58 PNGs (pipbpy_1.png a pipbpy_58.png) y se muestran
    // secuencialmente a VEL_MENU segundos por frame.
    // menuMostrado = true cuando se llega al último frame → aparece el menú.
    private com.jme3.ui.Picture fondoMenu;
    private com.jme3.texture.Texture2D[] framesMenu;
    private int totalFrames = 58;
    private float timerMenu = 0;
    private int frameActualMenu = 0;
    private final float VEL_MENU = 0.04f;
    private boolean menuMostrado = false;
 
    // --- Estado del Juego ---
    // enMenu: true mientras el menú esté activo; false mientras hay un modo en juego.
    // modoActivo: referencia al GameMode actual. null cuando se está en el menú.
    // ultimoScore / ultimoModo: guardan el puntaje y nombre del último modo jugado
    //   para mostrarlo en el menú como "ÚLTIMA PARTIDA".
    private boolean enMenu = true;
    private GameMode modoActivo = null;
    private int ultimoScore = -1;
    private String ultimoModo = "";
 
    // --- Música del Menú ---
    // musica5.wav reproducida en loop mientras el menú está activo.
    // Se detiene al lanzar un modo y se reanuda al volver con ESC.
    private AudioNode musicaMenu;
 
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
 
    @Override
    public void simpleInitApp() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
 
        flyCam.setEnabled(false);
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        cam.setLocation(new Vector3f(0, 12, 0));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Z);
 
        fondoMenu = new com.jme3.ui.Picture("FondoMenu");
        fondoMenu.setWidth(settings.getWidth());
        fondoMenu.setHeight(settings.getHeight());
        fondoMenu.setPosition(0f, 0f);
 
        framesMenu = new com.jme3.texture.Texture2D[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            int num = i + 1;
            String ruta = "Textures/Menu/pipbpy_" + num + ".png";
            framesMenu[i] = (com.jme3.texture.Texture2D) assetManager.loadTexture(ruta);
        }
 
        fondoMenu.setTexture(assetManager, framesMenu[0], true);
        guiNode.attachChild(fondoMenu);
 
        musicaMenu = new AudioNode(assetManager, "Sounds/musica5.wav", DataType.Buffer);
        musicaMenu.setLooping(true);
        musicaMenu.setVolume(0.5f);
        musicaMenu.setPositional(false);
        rootNode.attachChild(musicaMenu);
        musicaMenu.play();
 
        setupMenuKeys();
    }
 
    @Override
    public void simpleUpdate(float tpf) {
        if (enMenu && !menuMostrado && fondoMenu != null) {
            timerMenu += tpf;
            if (timerMenu >= VEL_MENU) {
                timerMenu = 0;
                frameActualMenu++;
                if (frameActualMenu >= totalFrames - 1) {
                    frameActualMenu = totalFrames - 1;
                    menuMostrado = true;
                    mostrarMenuHUD();
                }
                fondoMenu.setTexture(assetManager, framesMenu[frameActualMenu], true);
            }
        }
        if (!enMenu && modoActivo != null) {
            modoActivo.update(tpf);
        }
    }
 
    private void lanzarModo(int tipo) {
        enMenu = false;
        guiNode.detachAllChildren();
        fondoMenu.removeFromParent();
        if (musicaMenu != null) musicaMenu.stop();
 
        switch (tipo) {
            case 1 -> { modoActivo = new ModoClasico();       ultimoModo = "CLASICO";       }
            case 2 -> { modoActivo = new ModoAbanico();       ultimoModo = "ABANICO";       }
            case 3 -> { modoActivo = new ModoSupervivencia(); ultimoModo = "SUPERVIVENCIA"; }
            case 4 -> { modoActivo = new ModoZombies();       ultimoModo = "ZOMBIES";       }
            case 5 -> { modoActivo = new ModoInfierno();      ultimoModo = "INFIERNO";      }
        }
        modoActivo.iniciar(rootNode, guiNode, assetManager, inputManager, cam, guiFont);
    }
 
    private void volverAlMenu() {
        if (modoActivo != null) {
            ultimoScore = modoActivo.getScore();
            modoActivo.destruir();
            modoActivo = null;
        }
        enMenu = true;
        guiNode.detachAllChildren();
        frameActualMenu = 0;
        menuMostrado = false;
        fondoMenu.setTexture(assetManager, framesMenu[0], true);
        guiNode.attachChild(fondoMenu);
        if (musicaMenu != null) musicaMenu.play();
    }
 
    // --- HUD Y TEXTOS ---
    // mostrarMenuHUD(): construye el menú de selección sobre el fondo del Pip-Boy.
    //   Muestra la última partida jugada si ultimoScore != -1.
    // opcion(): helper para crear un BitmapText blanco centrado en pantalla.
    // centrar(): calcula la X necesaria para que un texto quede centrado horizontalmente.
 
    private void mostrarMenuHUD() {
        guiNode.detachAllChildren();
        guiNode.attachChild(fondoMenu);
 
        BitmapText t = texto("SELECCIONA MODO DE JUEGO", 30, ColorRGBA.Green);
        centrar(t, settings.getHeight() * 0.85f);
        guiNode.attachChild(t);
 
        guiNode.attachChild(opcion("1 - MODO CLASICO",                    settings.getHeight() * 0.70f));
        guiNode.attachChild(opcion("2 - MODO ABANICO",                    settings.getHeight() * 0.60f));
        guiNode.attachChild(opcion("3 - MODO SUPERVIVENCIA",              settings.getHeight() * 0.50f));
        guiNode.attachChild(opcion("4 - MODO ZOMBIES",                    settings.getHeight() * 0.40f));
        guiNode.attachChild(opcion("5 - MODO INFIERNO  [2 jugadores]",   settings.getHeight() * 0.30f));
 
        if (ultimoScore != -1) {
            BitmapText s = texto("ULTIMA PARTIDA (" + ultimoModo + "): " + ultimoScore, 20, ColorRGBA.Yellow);
            centrar(s, settings.getHeight() * 0.18f);
            guiNode.attachChild(s);
        }
    }
 
    private BitmapText opcion(String txt, float y) {
        BitmapText bt = texto(txt, 24, ColorRGBA.White);
        centrar(bt, y);
        return bt;
    }
 
    private BitmapText texto(String t, float size, ColorRGBA color) {
        BitmapText bt = new BitmapText(guiFont, false);
        bt.setSize(size);
        bt.setColor(color);
        bt.setText(t);
        return bt;
    }
 
    private void centrar(BitmapText bt, float y) {
        float x = (settings.getWidth() - bt.getLineWidth()) / 2;
        bt.setLocalTranslation(x, y, 0);
    }
 
    // --- CONTROLES ---
    // Teclas 1-5: seleccionan modo (solo funcionan cuando menuMostrado = true).
    // R: en ModoInfierno recarga balas de P2; en ModoZombies recarga munición;
    //    en cualquier otro modo (o con Game Over activo) → reinicia la partida.
    // P: en ModoInfierno recarga balas de P1.
    // ESC: en menú → cierra la app; en juego → vuelve al menú (llama destruir()).
 
    private void setupMenuKeys() {
        inputManager.addMapping("MODO1",      new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("MODO2",      new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("MODO3",      new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("MODO4",      new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("MODO5",      new KeyTrigger(KeyInput.KEY_5));
        // R → recarga P2 (WASD) en Infierno, o reinicia en otros modos
        inputManager.addMapping("RESTART",    new KeyTrigger(KeyInput.KEY_R));
        // P → recarga P1 (flechas) en Infierno
        inputManager.addMapping("RECARGA_P1", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("BACK",       new KeyTrigger(KeyInput.KEY_ESCAPE));
 
        inputManager.addListener(actionListener,
                "MODO1","MODO2","MODO3","MODO4","MODO5","RESTART","RECARGA_P1","BACK");
    }
 
    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
 
        if (enMenu && menuMostrado) {
            switch (name) {
                case "MODO1" -> lanzarModo(1);
                case "MODO2" -> lanzarModo(2);
                case "MODO3" -> lanzarModo(3);
                case "MODO4" -> lanzarModo(4);
                case "MODO5" -> lanzarModo(5);
                case "BACK"  -> stop();
            }
        } else if (!enMenu) {
            switch (name) {
                case "RESTART" -> {
                    if (modoActivo instanceof ModoInfierno && !modoActivo.isGameOver()) {
                        // R → recarga balas del Jugador 2 (WASD) en Infierno
                        ((ModoInfierno) modoActivo).recargarP2();
                    } else if (modoActivo instanceof ModoZombies && !modoActivo.isGameOver()) {
                        // R → recarga balas en ModoZombies
                        ((ModoZombies) modoActivo).recargarMunicion();
                    } else {
                        // R → reiniciar partida en cualquier otro modo o game over
                        if (modoActivo != null) modoActivo.onReiniciar();
                    }
                }
                case "RECARGA_P1" -> {
                    if (modoActivo instanceof ModoInfierno && !modoActivo.isGameOver()) {
                        // - → recarga balas del Jugador 1 (flechas) en Infierno
                        ((ModoInfierno) modoActivo).recargarP1();
                    }
                }
                case "BACK" -> volverAlMenu();
            }
        }
    };
}