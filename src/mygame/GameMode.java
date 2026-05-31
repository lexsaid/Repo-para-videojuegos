package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Contrato que deben cumplir todos los modos de juego.
 * El Main los instancia, les pasa los recursos compartidos
 * y los destruye cuando el jugador vuelve al menú.
 */
public interface GameMode {

    /**
     * Arranca el modo: construye escena, HUD, registra input.
     * Se llama una sola vez al entrar al modo.
     */
    void iniciar(Node rootNode, Node guiNode,
                 AssetManager assetManager,
                 InputManager inputManager,
                 Camera cam,
                 BitmapFont guiFont);

    /**
     * Llamado cada frame desde simpleUpdate del Main.
     * @param tpf tiempo entre frames (segundos)
     */
    void update(float tpf);

    /**
     * Reinicia la partida sin salir del modo.
     * Se llama al pulsar R.
     */
    void onReiniciar();

    /**
     * Destruye toda la escena del modo (geometrías, HUD, listeners).
     * Se llama al volver al menú con ESC.
     */
    void destruir();

    /** Puntuación acumulada en la sesión actual (100 pts por enemigo). */
    int getScore();

    /** Nombre corto del modo para mostrarlo en el menú. */
    String getNombreModo();

    /** True si la partida actual terminó en Game Over. */
    boolean isGameOver();

    /**
     * Balas restantes del modo actual.
     * Devuelve -1 por defecto → munición infinita (Clásico, Abanico, Supervivencia).
     * ModoZombies sobreescribe esto para devolver su contador real.
     */
    default int getBalasRestantes() { return -1; }
}