package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 *  MODO ABANICO
 * =========================================================
 *  · Velocidad jugador : lenta (2.5 — mitad de la normal)
 *  · Disparo           : 3 balas  (0°, +30°, −30°)
 *  · Cadencia          : ~1.5 salvas/s  (cooldown 0.67 s)
 *  · Vidas             : 3  (2 s invencibilidad al recibir daño)
 *  · Enemigos          : solo persiguen, NO disparan
 *  · Spawn inicial     : 6  |  Máximo en pantalla: 20
 *  · Suelo             : verde oscuro
 * =========================================================
 */
public class ModoAbanico extends ModoBase {

    @Override protected float  playerSpeed()     { return 2.5f;  } // mitad de velocidad
    @Override protected float  enemySpeed()      { return 1.8f;  }
    @Override protected float  shootCooldown()   { return 0.5f; } // 1.5× más que antes (era 1 s)
    @Override protected int    maxVidas()         { return 3;     }
    @Override protected int    maxEnemigos()      { return 30;    }
    @Override protected int    spawnInicial()     { return 6;     }
    @Override protected float  enemySpawnTime()   { return 1.5f;  }
    @Override protected boolean enemigosDisparan() { return false; }

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.08f, 0.18f, 0.08f, 1f); // verde oscuro
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(0.9f, 0.15f, 0.15f, 1f); // rojo
    }

    @Override
    protected String nombreModo() { return "Modo Abanico"; }

    /**
     * Disparo triple en abanico:
     *   · bala central  (  0°)
     *   · bala derecha  (+30°)
     *   · bala izquierda(−30°)
     *
     * crearBalaEnAngulo() está definido en ModoBase y rota
     * playerDir N grados alrededor del eje Y antes de disparar.
     */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(  0f);
        crearBalaEnAngulo(+30f);
        crearBalaEnAngulo(-30f);
    }
}