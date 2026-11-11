package io.github.jimbozoomer.jugcraft.nuclear;

/**
 * A DC equipment room's 125 VDC distribution panel (older rooms round 3; Justin: "DC distribution panels"): built as the
 * 120 VAC vital panels (VitalPanelBlock: the same enclosure, door and breakers, a click on the closed door opens it, on a
 * breaker switches it), hung on the room's side wall where the relay cabinets stood, labelled 1A-1, 1A-2, 1B-1, 1B-2.
 * Fed from its switchboard's feeder 03 (the -1 panels) or 04 (the -2): alive (POWER ON) while that feeder is and its
 * main is closed (DcRoom.panelLive). Its circuits feed the train's DC control loads (DcRoom.circuitName); what they draw
 * is the feeder's amps on the switchboard's laptop.
 */
public class DcPanelBlock extends VitalPanelBlock {
	public DcPanelBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected String circuit(int k, int v) {
		return DcRoom.circuitName(k, v);
	}
}
