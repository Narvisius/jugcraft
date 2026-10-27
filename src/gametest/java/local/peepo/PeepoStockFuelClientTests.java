package local.peepo;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Focused regression for a real item which can be both cooking ingredient and furnace fuel. */
public final class PeepoStockFuelClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){new PeepoStockStationsClientTests().fuelTest(context);}
}
