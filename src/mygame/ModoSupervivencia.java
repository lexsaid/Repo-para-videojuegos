package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 *  MODO SUPERVIVENCIA — El modo más difícil de los modos base
 * =========================================================
 *  Una sola vida: cualquier impacto (enemigo o bala enemiga) termina la partida.
 *  Los enemigos también disparan, creando dos amenazas simultáneas.
 *  El disparo del jugador es automático (mantener ESPACIO) con abanico triple.
 *
 *  Características:
 *    · Velocidad del jugador : normal (5.0 u/s)
 *    · Disparo               : abanico triple (−30°, 0°, +30°), automático al mantener ESPACIO
 *    · Cadencia              : 0.40 s entre salvas
 *    · Vidas                 : 1 sola vida — sin invencibilidad, cualquier golpe es Game Over
 *    · Enemigos              : persiguen Y DISPARAN cada 2.5 s hacia el jugador
 *    · Spawn inicial         : 6  |  Máximo: 20  |  Spawn cada 0.5 s (muy frecuente)
 *    · Escenario             : Escenario2.png con fondo rojo oscuro
 *    · Música                : musica3.wav
 *    · Texturas enemigos     : Textures/Enemigo_2/ con 4 frames (sobreescribe ModoBase)
 *
 *  disparoAutomatico() = true → update() llama intentarDisparar() cada frame
 *    mientras ESPACIO esté presionado, respetando el cooldown de 0.40 s.
 *  enemigosDisparan() = true → ModoBase activa actualizarDisparoEnemigos() en el update.
 * */
public class ModoSupervivencia extends ModoBase {

    @Override protected float    playerSpeed()      { return 5.0f;  }
    @Override protected float    enemySpeed()       { return 2.0f;  }
    @Override protected float    shootCooldown()    { return 0.40f; }
    @Override protected int      maxVidas()         { return 1;     }
    @Override protected int      maxEnemigos()      { return 20;    }
    @Override protected int      spawnInicial()     { return 6;     }
    @Override protected float    enemySpawnTime()   { return 0.5f;  }
    @Override protected boolean  enemigosDisparan() { return true;  }
    @Override protected float    enemyShootTime()   { return 2.5f;  }
    @Override protected boolean  disparoAutomatico() { return true;  }

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.2f, 0.04f, 0.04f, 1f); // rojo oscuro
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(1f, 0.38f, 0f, 1f); // naranja
    }

    @Override
    protected String nombreModo() { return "Modo Supervivencia"; }

    // ── NUEVO: Ruta de la música 3 para este Modo ────────────
    @Override
    protected String rutaMusica() {
        return "Sounds/musica3.wav"; // Si da problemas de formato, cámbialo a musica3.wav o musica3.ogg
    }

    // ── Rutas de texturas: Enemigo_2 ────────────────────────
    // Estos tres métodos hacen que ModoBase cargue las imágenes
    // de Textures/Enemigo_2/ en lugar de Textures/Enemigo_1/

    @Override
    protected String rutaEnemigoDerecha() {
        return "Textures/Enemigo_2/Enemy2Right_";
    }

    @Override
    protected String rutaEnemigoIzquierda() {
        return "Textures/Enemigo_2/Enemy2Left_";
    }

    @Override
    protected int cantidadSkinsEnemigo() {
        return 4; // Enemigo_2 tiene 4 frames (0 al 3)
    }

    /** Disparo triple en abanico. */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(  0f);
        crearBalaEnAngulo(+30f);
        crearBalaEnAngulo(-30f);
    }
}