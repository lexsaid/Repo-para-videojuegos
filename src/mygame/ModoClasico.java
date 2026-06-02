package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 *  MODO CLÁSICO — Modo introductorio, ideal para aprender los controles
 * =========================================================
 *  El modo más sencillo. Un solo disparo al frente, cadencia rápida
 *  y enemigos que solo persiguen sin disparar.
 *
 *  Características:
 *    · Velocidad del jugador : normal (5.0 u/s)
 *    · Disparo               : 1 bala directo al frente (ángulo 0°)
 *    · Cadencia              : 0.20 s entre disparos (la más rápida de los modos base)
 *    · Vidas                 : 3 (con 2 s de invencibilidad tras cada golpe)
 *    · Enemigos              : persiguen al jugador, NO disparan
 *    · Spawn inicial         : 6  |  Máximo simultáneo: 20
 *    · Escenario             : Escenario2.png (suelo marrón)
 *    · Música                : musica2.wav
 *    · Texturas enemigos     : Textures/Enemigo_1/ con 5 frames de animación
 *
 *  Esta clase solo sobreescribe los parámetros; toda la lógica está en ModoBase.
 * */
public class ModoClasico extends ModoBase {

    @Override protected float    playerSpeed()      { return 5.0f;  }
    @Override protected float    enemySpeed()       { return 1.8f;  }
    @Override protected float    shootCooldown()    { return 0.20f; }
    @Override protected int      maxVidas()         { return 3;     }
    @Override protected int      maxEnemigos()      { return 20;    }
    @Override protected int      spawnInicial()     { return 6;     }
    @Override protected float    enemySpawnTime()   { return 1.5f;  }
    @Override protected boolean  enemigosDisparan() { return false; }

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.15f, 0.15f, 0.2f, 1f); // gris azulado
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(0.9f, 0.1f, 0.1f, 1f); // rojo
    }

    @Override
    protected String nombreModo() { return "Modo Clasico"; }

    // ── NUEVO: Ruta de la música 2 para este Modo ────────────
    @Override
    protected String rutaMusica() {
        return "Sounds/musica2.wav"; // Si te da error de formato, cámbialo a musica2.wav
    }

    // ModoClasico usa Enemigo_1 → NO necesita sobreescribir las rutas.
    // ModoBase ya trae por defecto:
    //   rutaEnemigoDerecha()   = "Textures/Enemigo_1/enemy_"
    //   rutaEnemigoIzquierda() = "Textures/Enemigo_1/EnemyLeft_"
    //   cantidadSkinsEnemigo() = 5

    /** Disparo simple: 1 bala al frente. */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(0f);
    }
}