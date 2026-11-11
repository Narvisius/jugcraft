package io.github.jimbozoomer.jugcraft.warehouse;

/**
 * Part W5: a warehouse piece a comparator reads (0..15, from any of its blocks): Ready Storage how full its slots are,
 * a filling or emptying station its tank, a switchboard its stored power, the terminal its rack places in use.
 */
public interface AnalogSource {
	int analog();
}
