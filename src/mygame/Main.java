package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 * MAIN — Pip-Boy Animado con Selección de Modos
 */
public class Main extends SimpleApplication {

    // --- Variables de Animación ---
    private com.jme3.ui.Picture fondoMenu;
    private com.jme3.texture.Texture2D[] framesMenu;
    private int totalFrames = 99;
    private float timerMenu = 0;
    private int frameActualMenu = 0;
    private final float VEL_MENU = 0.04f;
    private boolean menuMostrado = false;

    // --- Estado del Juego ---
    private boolean enMenu = true;
    private GameMode modoActivo = null;
    private int ultimoScore = -1;
    private String ultimoModo = "";

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 1. CARGA DE FUENTE (Vital para que no crashee)
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // 2. CONFIGURACIÓN DE CÁMARA
        flyCam.setEnabled(false);
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        cam.setLocation(new Vector3f(0, 12, 0));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Z);

        // 3. PREPARAR EL FONDO (PICTURE)
        fondoMenu = new com.jme3.ui.Picture("FondoMenu");
        fondoMenu.setWidth(settings.getWidth());
        fondoMenu.setHeight(settings.getHeight());
        fondoMenu.setPosition(0f, 0f);

        // 4. CARGAR LAS 99 TEXTURAS
        framesMenu = new com.jme3.texture.Texture2D[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            int num = i + 1;
            String ruta = "Textures/Menu/PipBoy v2_40ms_" + num + ".png";
            framesMenu[i] = (com.jme3.texture.Texture2D) assetManager.loadTexture(ruta);
        }

        // Colocar el primer frame y mostrarlo
        fondoMenu.setTexture(assetManager, framesMenu[0], true);
        guiNode.attachChild(fondoMenu);

        // 5. CONFIGURAR TECLAS
        setupMenuKeys();
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Lógica de la Animación de Entrada
        if (enMenu && !menuMostrado && fondoMenu != null) {
            timerMenu += tpf;
            if (timerMenu >= VEL_MENU) {
                timerMenu = 0;
                frameActualMenu++;

                // Si llegamos al final de la animación (Frame 99)
                if (frameActualMenu >= totalFrames - 1) {
                    frameActualMenu = totalFrames - 1; // Se queda en la última foto
                    menuMostrado = true;
                    mostrarMenuHUD(); // Mostramos las letras de opciones
                }
                
                fondoMenu.setTexture(assetManager, framesMenu[frameActualMenu], true);
            }
        }

        // Update del juego si hay un modo activo
        if (!enMenu && modoActivo != null) {
            modoActivo.update(tpf);
        }
    }

    private void lanzarModo(int tipo) {
        enMenu = false;
        guiNode.detachAllChildren(); // Quitamos las letras del menú
        
        // ¡ESTO QUITA LA IMAGEN DEL PIP-BOY PARA QUE VEAS EL JUEGO!
        fondoMenu.removeFromParent(); 

        switch (tipo) {
            case 1 -> {
                modoActivo = new ModoClasico();
                ultimoModo = "CLASICO";
            }
            case 2 -> {
                modoActivo = new ModoAbanico();
                ultimoModo = "ABANICO";
            }
            case 3 -> {
                modoActivo = new ModoSupervivencia();
                ultimoModo = "SUPERVIVENCIA";
            }
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
        
        // Volvemos a poner el fondo y reiniciamos la animación desde el principio
        frameActualMenu = 0;
        menuMostrado = false;
        fondoMenu.setTexture(assetManager, framesMenu[0], true);
        guiNode.attachChild(fondoMenu);
    }

    // --- HUD Y TEXTOS ---

    private void mostrarMenuHUD() {
        guiNode.detachAllChildren();
        // Hay que re-añadir el fondo porque detachAllChildren lo quita
        guiNode.attachChild(fondoMenu);

        BitmapText t = texto("SELECCIONA MODO DE JUEGO", 30, ColorRGBA.Green);
        centrar(t, settings.getHeight() * 0.85f);
        guiNode.attachChild(t);

        guiNode.attachChild(opcion("1 - MODO CLASICO", settings.getHeight() * 0.70f));
        guiNode.attachChild(opcion("2 - MODO ABANICO", settings.getHeight() * 0.60f));
        guiNode.attachChild(opcion("3 - MODO SUPERVIVENCIA", settings.getHeight() * 0.50f));

        if (ultimoScore != -1) {
            BitmapText s = texto("ULTIMA PARTIDA (" + ultimoModo + "): " + ultimoScore, 20, ColorRGBA.Yellow);
            centrar(s, settings.getHeight() * 0.30f);
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

    private void setupMenuKeys() {
        inputManager.addMapping("MODO1",   new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("MODO2",   new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("MODO3",   new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("RESTART", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("BACK",    new KeyTrigger(KeyInput.KEY_ESCAPE));

        inputManager.addListener(actionListener, "MODO1","MODO2","MODO3","RESTART","BACK");
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;

        if (enMenu && menuMostrado) { // Solo funcionan las teclas si terminó la animación
            switch (name) {
                case "MODO1" -> lanzarModo(1);
                case "MODO2" -> lanzarModo(2);
                case "MODO3" -> lanzarModo(3);
                case "BACK"  -> stop();
            }
        } else if (!enMenu) {
            switch (name) {
                case "RESTART" -> { if (modoActivo != null) modoActivo.onReiniciar(); }
                case "BACK"    -> volverAlMenu();
            }
        }
    };
}