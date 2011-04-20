/**
 * 
 */
package smartdevices.controllers;

/**
 * @author jsnape
 *
 */
public enum WattboxUserProfile {
VERY_WARM(24, 0),
WARM (23.5, 0.5),
EFFICIENT (22.6, 0.75),
EXTRA_EFFICIENT (22, 1);

private final double setPoint;   // in kilograms
private final double profileVariation; // in meters

WattboxUserProfile(double setPoint, double profileVariation)
{
	this.setPoint = setPoint;
	this.profileVariation = profileVariation;
}

public double getSetPoint()   { return this.setPoint; }
public double getVariation() { return this.profileVariation; }


}
