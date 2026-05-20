package mygame;

import com.jme3.math.ColorRGBA;

/**
 * =========================================================
 *  MODO SUPERVIVENCIA
 * =========================================================
 *  · Velocidad jugador : normal (5.0)
 *  · Disparo           : 3 balas  (0°, +30°, −30°)
 *  · Cadencia          : ~1.5 salvas/s  (cooldown 0.67 s)
 *  · Vidas             : 1  (cualquier impacto = Game Over)
 *  · Enemigos          : persiguen Y disparan cada 2.5 s
 *                        Sus balas (magenta) van al jugador
 *  · Spawn inicial     : 6  |  Máximo en pantalla: 20
 *  · Suelo             : rojo oscuro (señal de peligro)
 *  · Enemigos          : naranja (diferente al modo clásico)
 * =========================================================
 */
public class ModoSupervivencia extends ModoBase {

    @Override protected float  playerSpeed()      { return 5.0f;  }
    @Override protected float  enemySpeed()       { return 2.0f;  } // un poco más rápidos
    @Override protected float  shootCooldown()    { return 0.40f; } // 1.5× más que antes (era 1 s)
    @Override protected int    maxVidas()          { return 1;     } // 1 sola vida
    @Override protected int    maxEnemigos()       { return 20;    }
    @Override protected int    spawnInicial()      { return 6;     }
    @Override protected float  enemySpawnTime()    { return 0.5f;  }
    @Override protected boolean enemigosDisparan() { return true;  } // ← activar disparo enemigo
    @Override protected float  enemyShootTime()    { return 2.5f;  } // segundos entre disparos

    @Override
    protected ColorRGBA colorArena() {
        return new ColorRGBA(0.2f, 0.04f, 0.04f, 1f); // rojo oscuro = peligro
    }

    @Override
    protected ColorRGBA colorEnemigo() {
        return new ColorRGBA(1f, 0.38f, 0f, 1f); // naranja
    }

    @Override
    protected String nombreModo() { return "Modo Supervivencia"; }

    /**
     * Disparo triple igual que ModoAbanico.
     * El mayor desafío en este modo es esquivar las balas
     * enemigas mientras se gestiona la cadencia propia.
     */
    @Override
    protected void crearDisparoJugador() {
        crearBalaEnAngulo(  0f);
        crearBalaEnAngulo(+30f);
        crearBalaEnAngulo(-30f);
    }
}