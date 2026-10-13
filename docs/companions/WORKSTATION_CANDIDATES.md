# Potential assigned workstations

Inspected local checkout of GitHub Jugcraft main ca938b54 on 2026-10-06. These are implementation proposals, not enabled interactions.

| Station | Possible assistance | Existing implementation |
|---|---|---|
| Cooking Pot | Stirring adds cooking progress while heated, with a valid recipe and output room | agriculture/CookingPotBlockEntity.java |
| Canning Kettle | Faster jar processing while the water is boiling; keep heat/water/jar checks | agriculture/CanningKettleBlockEntity.java |
| Cider Press | Turn the grinder and press automatically, respecting paced work, apple/pulp capacity and juice space | agriculture/CiderPressBlockEntity.java |
| Candy Kettle | Attend a selected candy stage and help with handling; a simple speed buff could overshoot the desired temperature | agriculture/CandyKettleBlockEntity.java |
| Electric Furnace, Crusher, Alloy Smelter | Increase processing progress with valid inputs, output space and power | machine/MachineBlockEntity.java |
| Metal Press, Wire Drawer, Circuit Assembler | Speed manufacturing under the same recipe and energy checks | machine/MachineBlockEntity.java |
| Pulverizer, Ore Washer, Sieve, Sawmill | Speed processing while retaining byproducts and any fluid requirements | machine/MachineBlockEntity.java |
| Coke Oven, Steel Foundry, Arc Furnace | Assist processing without bypassing fuel, structure, gas or power requirements | machine/MachineBlockEntity.java |
| Auto-Crafter | Faster crafting while respecting its pattern, ingredient and remainder slots | machine/MachineBlockEntity.java |

Paths above are under src/main/java/io/github/jimbozoomer/jugcraft/.

Recommended first implementation: one assigned helper per Cooking Pot, +25% progress while actively helping, costing companion energy. Persist the assignment by dimension and block position; validate access, distance and station existence. Pause on exhaustion, missing inputs, blocked output or unavailable heat/power. Resume after recovery. Share the existing food/rest routine and make assignment replace opportunistic wheel work. Avoid stacking helpers or granting free machine energy: charge energy for additional powered processing and account for existing speed upgrades. Workstation block entities need adapters/hooks; the present wheel interface alone does not accelerate recipes.
