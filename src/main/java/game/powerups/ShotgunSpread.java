package game.powerups;

import game.Config;
import game.Progress;
import game.weapons.WeaponKind;
import game.weapons.Shotgun;

public class ShotgunSpread implements PowerUp {
    public void apply() {
        Shotgun shotgun = (Shotgun) WeaponKind.SHOTGUN.get();
        shotgun.setSpreadInRadians(
            (float) (shotgun.getSpreadInRadians() + Math.PI / 128)
        );
    }
    
    public boolean isAllowedToUse() {
        Shotgun shotgun = (Shotgun) WeaponKind.SHOTGUN.get();
        return Progress.unlockedWeapons.contains(WeaponKind.SHOTGUN) && shotgun.getSpreadInRadians() < Config.MAX_SHOTGUN_SPREAD_RADIANS;
    }
}