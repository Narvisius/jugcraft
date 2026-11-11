package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * The pre-made plants' building kit (docs/features/nuclear.md, "Pre-made plants"): what the buildings are fitted out
 * with, inside and out. Doors that open by hand, a roller door that opens as one, building and room signs, server
 * racks, relay cabinets and pallet racking that join up when stacked, office furniture, safety railing, ducts, and
 * the plain finishes (suspended ceiling, vinyl and epoxy floors, painted block walls, painted tank plate, louvres).
 * Same ids as tools/plant_interior.py.
 */
public final class PlantInterior {
	/**
	 * Furniture: id, light, kind ("plain"; "stack" joins up when stacked; "row:group" joins side by side with the
	 * group; "variant:n" comes out as one of n at random), material ("metal", "wood", "wool", "plant"), then its
	 * hitbox (pixels, facing north).
	 */
	private static final Object[][] FURNITURE = {
			{"relay_cabinet", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15.3}},
			{"pallet_rack", 0, "stack", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"pallet_rack_drums", 0, "stack", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"storage_rack", 0, "rack", "metal", null},
			{"high_bay_light", 15, "high_bay", "metal", new double[] {2.5, 7.8, 2.5, 13.5, 16, 13.5}},
			{"light_hanger", 0, "plain", "metal", new double[] {6.5, 0, 6.5, 9.5, 16, 9.5}},
			{"office_desk", 0, "deskpc", "wood", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"filing_cabinet", 0, "plain", "metal", new double[] {2, 0, 2, 14, 15, 11}},   // (tools/plant_filing: 53 cm deep)
			{"whiteboard", 0, "variant:24", "metal", new double[] {0.5, 3, 0, 15.5, 15, 1.6}},
			{"water_cooler", 0, "plain", "metal", new double[] {4, 0, 4, 12, 16, 12}},
			{"guard_rail", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 2}},
			{"locker", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"locker_open", 0, "locker:lab", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			// the drone tower's crew locker (tools/plant_lockers.py, redesign 4): its jacket and kit its own, nothing to take
			{"crew_locker_open", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"bookcase", 0, "stack", "wood", new double[] {1, 0, 1, 15, 16, 15}},
			{"fridge", 0, "stack", "metal", new double[] {1.5, 0, 2, 14.5, 16, 15}},
			{"fume_hood", 0, "stack", "metal", new double[] {0, 0, 1, 16, 16, 15.5}},
			{"reception_desk", 0, "deskrow:reception:0:3:0", "wood", new double[] {0, 0, 0, 16, 16, 16}},
			{"sofa", 0, "row:sofa:0.42", "wool", new double[] {0, 0, 1, 16, 8, 16}},
			{"locker_bench", 0, "row:bench:0.4", "wood", new double[] {0, 0, 4, 16, 7, 12}},
			{"kitchen_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"kitchen_sink", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"microwave_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"coffee_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"lab_bench", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"lab_sink", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"lab_analyzer", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"cctv_desk", 0, "plain", "metal", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"coffee_table", 0, "plain", "wood", new double[] {1, 0, 3, 15, 7, 13}},
			{"cafe_table", 0, "variant:6", "wood", new double[] {2, 0, 2, 14, 12.5, 14}},
			{"cafe_chair", 0, "seat:0.44", "wood", new double[] {3.5, 0, 3.5, 12.5, 9, 13}},
			{"coat_rack", 0, "plain", "wood", new double[] {1, 0, 0, 15, 16, 4}},
			{"printer", 0, "plain", "metal", new double[] {1, 0, 2, 15, 14, 14}},
			{"wall_display", 0, "plain", "metal", new double[] {0, 4, 0, 16, 14, 1.2}},
			{"office_plant", 0, "variant:8", "plant", new double[] {4, 0, 4, 12, 16, 12}},
			{"framed_print", 0, "variant:16", "wood", new double[] {1, 3, 0, 15, 14, 1.6}},
			{"locker_security", 0, "locker:security", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"office_desk_long", 0, "deskrow:desk:0:6:0,4", "wood", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"executive_desk", 0, "deskrow:exec:0:3:0", "wood", new double[] {0, 0, 0.5, 16, 13, 15.5}},
			{"glass_desk", 0, "deskrow:glass:0:4:0", "metal", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"operator_console", 4, "row:opcon:0:4", "metal", new double[] {0, 0, 0.5, 16, 12, 15.5}},
			{"stretcher", 0, "row:stretcher:0.6", "metal", new double[] {0, 0, 1.5, 16, 10, 14.5}},
			{"cubicle_panel", 0, "row:cubicle", "wool", new double[] {0, 0, 7, 16, 16, 9}},
			{"aed_cabinet", 0, "plain", "metal", new double[] {3, 3, 0, 13, 14, 4}},
			{"fire_extinguisher", 0, "plain", "metal", new double[] {3, 0.5, 0, 13, 15.5, 4.6}},
			{"emergency_light", 0, "plain", "metal", new double[] {1.4, 0.8, 0, 14.6, 14.8, 12}},
			{"emergency_light_conduit", 0, "plain", "metal", new double[] {7.4, 0, 0, 9.6, 16, 1.8}},
			{"medicine_cabinet", 0, "plain", "metal", new double[] {1, 5, 0, 15, 16, 5}},
			{"vitals_monitor", 0, "plain", "metal", new double[] {4, 0, 4, 12, 16, 12}},
			{"filing_cabinet_open", 0, "plain", "metal", new double[] {2, 0, 2, 14, 15, 11}},
			{"drafting_table", 0, "plain", "wood", new double[] {0, 0, 1, 16, 16, 15}},
			{"blueprint_sheet", 0, "variant:6", "wood", new double[] {1, 2, 0, 15, 15, 0.6}},
			{"training_screen", 0, "multi:3:2:v", "metal", null},
			{"plant_map_screen", 0, "overview", "metal", null},
			{"screen_raceway", 0, "plain", "metal", new double[] {5.6, 0.8, 0, 10.4, 16, 1.3}},
			{"status_screen", 0, "status", "metal", null},
			{"oval_table", 0, "multi:5:3:h:3", "wood", null},
			{"lounge_table", 0, "multi:3:2:c", "glass", null},
			{"long_glass_table", 0, "multi:3:1:c", "glass", null},
			{"large_print", 0, "multi:2:1:v:12", "wood", null},
			{"filing_cabinet_tall", 0, "stack:tall_filing", "metal", new double[] {2, 0, 2, 14, 16, 15}},
			{"filing_cabinet_tall_open", 0, "stack:tall_filing", "metal", new double[] {2, 0, 2, 14, 16, 15}},
			{"office_copier", 0, "multi:2:2:b", "metal", null},
			{"glass_coffee_table", 0, "steam:4.8:9.8:6.8", "metal", new double[] {1, 0, 2, 15, 7.3, 14}},
			// The diesel generator rooms' control and protection panels (tools/diesel_panels.py, round 31 part 2): two high,
			// one or two wide, labelled by train (EDG 1A, EDG 1B); the engine, generator and protection panels' controls
			// work (EdgPanelBlock, DieselPanels); the battery is two wide.
			{"edg_engine_panel", 0, "edgpanel:1:0", "metal", null},
			{"edg_generator_panel", 0, "edgpanel:2:1", "metal", null},
			{"edg_voltage_regulator", 0, "live:1:2:p:2", "metal", null},
			{"edg_protection_panel", 0, "edgpanel:1:2", "metal", null},
			{"edg_load_sequencer", 0, "live:1:2:p:2", "metal", null},
			{"edg_battery_charger", 0, "live:1:2:p:2", "metal", null},
			{"edg_battery_rack", 0, "live:2:2:p:2", "metal", null},
			// The diesel rooms' fittings (tools/diesel_fittings.py, round 31 part 3): on the walls, live, linked to their room's
			// generator: the CO2 release station (Co2StationBlock), the horns and strobes, the exhaust fans (two wide: as many
			// as a room has), the hoist's pendant station (HoistPendantBlock); the CO2 piping under the roof and its nozzles,
			// the CO2 and hearing protection signs.
			{"edg_co2_station", 0, "co2station", "metal", null},
			{"edg_horn_strobe", 0, "wall:1:4.4", "metal", null},
			{"edg_room_fan", 0, "wallw:2:2:5.8", "metal", null},
			{"edg_hoist_pendant", 0, "pendant", "metal", null},
			{"co2_pipe", 0, "plain", "metal", new double[] {7, 10, 0, 9, 12, 16}},
			{"co2_pipe_hanger", 0, "plain", "metal", new double[] {6.6, 9.4, 0, 9.4, 16, 16}},
			{"co2_nozzle", 0, "plain", "metal", new double[] {6.5, 5.9, 0, 9.5, 12.4, 16}},
			{"co2_nozzle_end", 0, "plain", "metal", new double[] {6.5, 5.9, 0, 9.5, 12.4, 10.2}},
			{"co2_riser", 0, "plain", "metal", new double[] {7, 0, 7, 9, 16, 9}},
			{"co2_elbow", 0, "plain", "metal", new double[] {6.6, 9.6, 0, 16, 12.4, 9.4}},
			{"co2_elbow_down", 0, "plain", "metal", new double[] {6.6, 0, 0, 9.4, 12.4, 9.4}},
			{"co2_tee", 0, "plain", "metal", new double[] {6.6, 9.6, 0, 16, 12.4, 16}},
			{"sign_co2", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_hearing", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The control building's DC system (tools/plant_panels.py, round 18): the station battery (a four-block step
			// rack of 30 cells), its distribution switchboard, the vital inverter, and the cables from battery to switchboard.
			{"station_battery", 0, "battery:4:2:p:8", "metal", null},
			{"dc_switchboard", 0, "dcboard", "metal", null},
			{"vital_inverter", 0, "dcinv", "metal", null},
			// The DC rooms' own battery chargers (round 26, 8b): live like the diesel rooms', a model of their own.
			{"station_charger", 0, "dccharger", "metal", null},
			// Each vital inverter's regulating transformer, its bypass (round 26, 8d): beside its inverter, live, reading its
			// MCC's REG XF breaker.
			{"reg_transformer", 0, "live:1:2:p:4", "metal", null},
			// Each inverter's 120 VAC vital instrument panel (round 26, 8e): hung on a wall, its door opens and its breakers
			// switch (VitalPanelBlock).
			{"vital_panel", 0, "vital", "metal", null},
			// Each DC room's ground detector and battery monitor (round 26, 8f): at the end of the equipment row, its test
			// buttons held to put a ground on the bus (GroundDetectorBlock).
			{"dc_ground_detector", 0, "ground", "metal", null},
			// The DC rooms brought up to the detail standard (older rooms round 3; tools/dc_room.py, DcRoom): the switchboard's
			// breakers, the chargers' breakers, MODE and annunciator buttons, the inverters' bypass switch, breaker and buttons
			// work; the 125 VDC distribution panels (built as the vital panels: DcPanelBlock); the log desk's laptop; the
			// one-line diagram, the battery service board, the test cart, the signs.
			{"dc_dist_panel", 0, "dcpanel", "metal", null},
			{"dc_desk_laptop", 0, "dclaptop", "metal", null},
			{"dc_one_line", 0, "multi:3:2:v", "wood", null},
			{"battery_service_board", 0, "multi:2:1:v", "wood", null},
			{"battery_test_cart", 0, "plain", "metal", new double[] {1.4, 0, 2.4, 14.6, 16, 13.8}},
			{"sign_dc_train_a", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_dc_train_b", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_dc_danger", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The battery rooms brought up to the detail standard (older rooms round 4; tools/battery_rooms.py): each string's
			// disconnect switch (BatteryDisconnectBlock) and its conduit up into the tray; the vapor-tight lights; the PPE
			// cabinet, the cell log, the neutralization sump, the room's signs.
			{"battery_disconnect", 0, "bdisc", "metal", null},
			{"battery_disconnect_riser", 0, "multi:1:2:v", "metal", null},
			{"vapor_tight_light", 15, "plain", "metal", new double[] {1, 12.2, 4.7, 15.8, 16, 11.3}},
			{"battery_ppe_cabinet", 0, "plain", "metal", new double[] {1, 1, 0, 15, 15, 5.8}},
			{"battery_log_board", 0, "plain", "wood", new double[] {1.2, 1, 0, 14.8, 15, 1.2}},
			{"battery_room_sump", 0, "plain", "metal", new double[] {0.6, 0, 0.6, 15.4, 0.35, 15.4}},
			{"sign_battery_1a", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_battery_1b", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The main control room's furniture to the detail standard (older rooms round 5; tools/cr_room.py): the
			// operators' consoles and the shift manager's desk (CrConsoleBlock: live screens, ControlRoomConsoles), the
			// procedure racks (eight sections), the SCBA cabinet, the EAL chart, the At The Controls signs and floor line.
			{"cr_console", 4, "crcon:4", "metal", null},
			{"ss_desk", 4, "crcon:1", "metal", null},
			{"cr_procedure_rack", 0, "multi:1:2:p:8", "metal", null},
			{"cr_scba_cabinet", 0, "multi:1:2:p", "metal", null},
			{"cr_eal_chart", 0, "multi:2:1:v", "wood", null},
			{"cr_atc_sign", 0, "plain", "metal", new double[] {1, 0, 4.6, 15, 15.6, 11.4}},
			{"cr_atc_line", 0, "plain", "wool", new double[] {0, 0, 6.6, 16, 0.2, 9.4}},
			// The entrance hall to the detail standard (older rooms round 6; tools/entrance_hall.py): the security post
			// (AcStationBlock: the gates' access log, the gate panel, the duress button; AccessControl), the hand reader,
			// the evacuation plan, the signs over the vestibule's inner doors.
			{"ac_station", 2, "acpost", "metal", null},
			{"ac_hand_reader", 0, "achand", "metal", new double[] {3.6, 0, 3.2, 12.4, 21, 12.8}},
			{"ac_evac_plan", 0, "multi:2:1:v", "wood", null},
			{"sign_vital_area", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_prohibited", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The remote shutdown room to the detail standard (older rooms round 7; tools/rsr_room.py): the remote shutdown
			// station (CrConsoleBlock: live screens, AOP-8's checks; ControlRoomConsoles), the transfer key cabinet, the
			// communications station, the room's sign.
			{"rsr_desk", 4, "crcon:1", "metal", null},
			{"rsr_key_cabinet", 0, "plain", "metal", new double[] {3, 2, 0, 14.6, 14, 2.6}},
			{"rsr_phone_station", 0, "plain", "metal", new double[] {1.6, 2.4, 0, 14.6, 13.6, 4.2}},
			{"sign_rsr_area", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The security gatehouse to the detail standard (tools/sec_gatehouse.py): the alarm station console, the X-ray
			// baggage scanner, the walk-through metal detector (SecurityGatehouse), the weapons locker, the dome camera,
			// the PROTECTED AREA sign.
			{"sec_alarm_console", 4, "secsas", "metal", null},
			{"sec_xray", 0, "secxray", "metal", null},
			{"sec_metal_detector", 0, "secwtmd", "metal", null},
			{"sec_weapons_locker", 0, "multi:1:2:p", "metal", null},
			{"sec_dome_camera", 0, "plain", "metal", new double[] {4.8, 12.8, 4.8, 11.2, 16, 11.2}},
			{"sign_protected_area", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The administration building part 1 (tools/admin_fittings.py): the EXIT sign (lit), the fire alarm pull
			// station (FireAlarmPullBlock), the floors' evacuation plans; the office computers' screens are live
			// (DeskComputerBlock, DeskComputerRowBlock: DeskScreenBlockEntity).
			{"exit_sign", 8, "plain", "metal", new double[] {2, 4, 0, 14, 10, 2.4}},
			{"fire_alarm_pull", 0, "pull", "metal", new double[] {5, 3, 0, 11, 12, 3.2}},
			{"admin_evac_plan", 0, "multi:2:1:v:3", "wood", null},
			// The administration building part 2 (tools/admin_rooms.py): the chemistry lab's status board and gamma
			// spectroscopy station (ChemLab), its safety cabinets (doors open on a click), the emergency shower and eyewash,
			// the lab's sign; document control's issue counter (its computer live) and sign (the shelving and the plan file
			// are registered below); the board room's table, AV credenza and lectern; the unit performance screens; the
			// plant manager's direct line to the control room, his license, awards and hard hat hooks.
			{"chem_status_board", 0, "chemboard", "metal", null},
			{"chem_gamma_spec", 0, "gammaspec", "metal", null},
			{"chem_flam_cabinet", 0, "safetycab", "metal", null},
			{"chem_acid_cabinet", 0, "safetycab", "metal", null},
			{"lab_safety_shower", 0, "shower", "metal", null},
			{"sign_chem_lab", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"doc_issue_counter", 0, "deskrow:doccounter:0:3:0", "wood", new double[] {0, 0, 0, 16, 16, 16}},
			{"sign_doc_control", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"board_table", 0, "multi:8:3:h", "wood", null},
			{"board_av_credenza", 0, "multi:3:1:b", "wood", null},
			{"board_lectern", 0, "plain", "wood", new double[] {2, 0, 3, 14, 19.6, 13.6}},
			{"perf_screen", 0, "perf", "metal", null},
			{"mgr_hotline", 0, "hotline", "metal", null},
			{"mgr_license", 0, "multi:2:1:v", "wood", null},
			{"mgr_awards", 0, "multi:3:1:v", "wood", null},
			{"mgr_ppe_hooks", 0, "plain", "wood", new double[] {1, 1.6, 0, 15, 12.3, 7.6}},
			// The battery rooms' (round 26, #9): the hydrogen monitor and the exhaust fan on the walls (live, labelled by
			// train), the eyewash (EyewashBlock), the spill kit, the hydrogen sensor head, the signs; the second strings'
			// cable runs with the battery monitor's sense cable.
			{"h2_monitor", 0, "wall:2:3.8", "metal", null},
			{"exhaust_fan", 0, "fan", "metal", null},
			{"eyewash_station", 0, "eyewash", "metal", new double[] {2.5, 4.6, 0, 13.5, 13.8, 13}},
			{"spill_kit", 0, "plain", "metal", new double[] {1.7, 1, 0, 14.3, 14.5, 6.6}},
			{"h2_sensor", 0, "plain", "metal", new double[] {6.4, 0, 0.2, 12.4, 10.5, 3.6}},
			{"sign_no_smoking", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_battery_acid", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_hydrogen", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_eyewash", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"dc_cable_tray", 0, "plain", "metal", new double[] {0, 12, 4, 16, 16, 12}},
			// The relay room (round 26, #10: RelayRoom): its cabinets labelled and reading the reactor; its control cable
			// tray, the bundles down into the cabinets, the fire stops, the row signs, the smoke detectors, the drawing
			// rack and the sequence of events recorder's printer.
			{"protection_rack", 0, "relay:12", "metal", null},
			{"ssps_cabinet", 0, "relay:6", "metal", null},
			{"nis_cabinet", 0, "relay:5", "metal", null},
			{"relay_panel", 0, "relay:29", "metal", null},
			{"control_tray", 0, "plain", "metal", new double[] {0, 11.2, 3, 16, 14, 13}},
			{"control_tray_hanger", 0, "plain", "metal", new double[] {0, 10.2, 1.4, 16, 14, 14.6}},
			{"control_tray_waterfall", 0, "plain", "metal", new double[] {0, 10.4, 3, 16, 14, 13}},
			{"control_tray_waterfall_hanger", 0, "plain", "metal", new double[] {0, 10.2, 1.4, 16, 14, 14.6}},
			{"control_tray_cross", 0, "plain", "metal", new double[] {0, 11.2, 0, 16, 14, 16}},
			{"control_tray_tee", 0, "plain", "metal", new double[] {0, 11.2, 3, 16, 14, 16}},
			{"control_tray_firestop", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"control_cable_drop", 0, "plain", "metal", new double[] {6.6, 0, 6.6, 9.4, 16, 9.4}},
			{"control_cable_entry", 0, "plain", "metal", new double[] {5.2, 0, 5.2, 10.8, 16, 10.8}},
			{"smoke_detector", 0, "plain", "metal", new double[] {5.3, 13.6, 5.3, 10.7, 16, 10.7}},
			{"drawing_rack", 0, "plain", "metal", new double[] {0.6, 0, 2, 15.4, 15.2, 14}},
			{"soe_printer", 0, "plain", "metal", new double[] {0.5, 0, 1.5, 15.5, 14.6, 14.5}},
			{"sign_relay_row_a", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_relay_row_b", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_relay_row_c", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_relay_row_d", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"dc_cable_riser", 0, "plain", "metal", new double[] {5, 0, 0, 11, 16, 3}},
			{"dc_cable_riser_top", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 12}},
			{"dc_cable_riser_sense", 0, "plain", "metal", new double[] {5, 0, 0, 13, 16, 3}},
			{"dc_cable_riser_top_sense", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 12}},
			{"dc_cable_tray_bend", 0, "plain", "metal", new double[] {0, 12, 4, 12, 16, 16}},
			{"dc_cable_tray_tee", 0, "plain", "metal", new double[] {0, 12, 4, 16, 16, 16}},
			// A load centre's feeder conduit, up from its hub through the ceiling into the trays above (round 25b).
			{"feeder_conduit", 0, "plain", "metal", new double[] {6.7, 0, 6.7, 9.3, 16, 9.3}},
			{"feeder_conduit_base", 0, "plain", "metal", new double[] {6.7, 0, 6.7, 9.3, 16, 9.3}},
			{"feeder_conduit_ceiling", 0, "plain", "metal", new double[] {6.7, 0, 6.7, 9.3, 16, 9.3}},
			{"feeder_conduit_floor", 0, "plain", "metal", new double[] {5.6, 0, 0, 10.4, 16, 10.4}},
			{"feeder_conduit_tray", 0, "plain", "metal", new double[] {6.5, 0, 6.5, 9.5, 8.8, 16}},
			// The DC rooms' overhead ladder tray and the REG XF feeders' conduits (round 26, 8d). Facing north a piece runs
			// along x; the bend joins the west side to the south side.
			{"ladder_tray", 0, "plain", "metal", new double[] {0, 11.2, 3, 16, 14, 13}},
			{"ladder_tray_hanger", 0, "plain", "metal", new double[] {0, 9.6, 1.4, 16, 16, 14.6}},
			{"ladder_tray_bend", 0, "plain", "metal", new double[] {0, 11.2, 3, 13, 14, 16}},
			{"ladder_tray_drop", 0, "plain", "metal", new double[] {0, 0, 3, 16, 14, 13}},
			{"ladder_tray_drop_pair", 0, "plain", "metal", new double[] {0, 0, 3, 16, 14, 13}},
			{"conduit_pair", 0, "plain", "metal", new double[] {3, 0, 6.5, 13, 16, 9.5}},
			// Every cabinet in the DC rooms' rows with its conduits up into the tray (round 26, 8f): an inverter's two, the
			// drops on their hangers, the bend with the ground detector's drop.
			{"conduit_pair_run", 0, "plain", "metal", new double[] {3, 0, 6.5, 13, 16, 9.5}},
			{"ladder_tray_drop_hanger", 0, "plain", "metal", new double[] {0, 0, 1.4, 16, 16, 14.6}},
			{"ladder_tray_drop_pair_hanger", 0, "plain", "metal", new double[] {0, 0, 1.4, 16, 16, 14.6}},
			{"ladder_tray_bend_drop", 0, "plain", "metal", new double[] {0, 0, 3, 13, 14, 16}},
			// A vital panel's conduit up the wall to the tray, and the tray's drop over it (round 26, 8e): the wall at z 0.
			{"wall_conduit", 0, "plain", "metal", new double[] {7.2, 0, 1.8, 8.8, 16, 3.4}},
			{"ladder_tray_drop_wall", 0, "plain", "metal", new double[] {0, 0, 1.8, 16, 14.8, 13}},
			// The Control Room HVAC Equipment Room (round 26, #11: HvacRoom, tools/hvac_room.py): its ducts, dampers (opened and
			// shut by its panel), intake plenums, chilled water pipes, the south bay's tanks, feed pots, sensors and spare filters,
			// the HVAC control panel and the wall monitors.
			{"hv_duct", 0, "plain", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_duct_hanger", 0, "plain", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_duct_riser", 0, "plain", "metal", new double[] {1.5, 0, 1.5, 14.5, 16, 14.5}},
			{"hv_duct_elbow", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_elbow_down", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_elbow_up", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_tee", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_tee_down", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_sleeve", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_riser_fd", 0, "plain", "metal", new double[] {1.5, 0, 1.5, 14.5, 16, 14.5}},
			{"hv_supply_diffuser", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_return_grille", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_chw_pipe", 0, "plain", "metal", new double[] {6, 6, 0, 10, 10, 16}},
			{"hv_chw_pipe_hanger", 0, "plain", "metal", new double[] {6, 6, 0, 10, 10, 16}},
			{"hv_chw_pipe_trapeze", 0, "plain", "metal", new double[] {6, 6, 0, 10, 10, 16}},
			{"hv_chw_riser", 0, "plain", "metal", new double[] {6, 0, 6, 10, 16, 10}},
			{"hv_chw_elbow", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_chw_elbow_down", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_chw_elbow_up", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_chw_tee", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"hv_duct_label_oa", 0, "plain", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_duct_label_filtered", 0, "plain", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_duct_label_supply", 0, "plain", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_duct_label_return", 0, "plain", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_duct_riser_label_supply", 0, "plain", "metal", new double[] {1.5, 0, 1.5, 14.5, 16, 14.5}},
			{"hv_duct_riser_label_return", 0, "plain", "metal", new double[] {1.5, 0, 1.5, 14.5, 16, 14.5}},
			{"hv_chw_label_chws", 0, "plain", "metal", new double[] {6, 6, 0, 10, 10, 16}},
			{"hv_chw_label_chwr", 0, "plain", "metal", new double[] {6, 6, 0, 10, 10, 16}},
			{"hv_plenum_base", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum_outlet", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum_door_a", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum_door_b", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum_detector", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum_top", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_plenum_top_outlet", 0, "row:hv_plenum", "metal", new double[] {0, 0, 0, 16, 16, 15.8}},
			{"hv_expansion_tank", 0, "plain", "metal", new double[] {2.6, 0, 2.6, 13.4, 16, 13.4}},
			{"hv_expansion_tank_top", 0, "plain", "metal", new double[] {3, 0, 3, 13, 16, 13}},
			{"hv_small_pipe", 0, "plain", "metal", new double[] {6.6, 0, 6.6, 9.4, 16, 9.4}},
			{"hv_chw_pipe_tap", 0, "plain", "metal", new double[] {6, 0, 0, 10, 10, 16}},
			{"hv_chem_feeder", 0, "plain", "metal", new double[] {5, 0, 5, 11, 16, 11}},
			{"hv_refrig_sensor", 0, "plain", "metal", new double[] {5, 0, 5, 11, 16, 11}},
			{"hv_filter_rack", 0, "variant:4", "metal", new double[] {0.5, 0, 1, 15.5, 16, 13.8}},
			{"hv_damper", 0, "damper", "metal", new double[] {1.5, 1.5, 0, 14.5, 14.5, 16}},
			{"hv_control_panel", 0, "hvpanel", "metal", null},
			{"hv_rad_monitor", 0, "hvmon:2:4.2", "metal", null},
			{"hv_tox_monitor", 0, "hvmon:2:4.2", "metal", null},
			{"hv_refrig_monitor", 0, "hvmon:1:4.2", "metal", null},
			// The Technical Support Center (round 26, #12: TscRoom, tools/tsc_room.py): its classification board, video wall
			// displays, consoles, communications console, NRC desk, status boards, area radiation monitor and frisking
			// station (live: tsc:width:rows:variants), its procedure shelves, flat file of drawings and card reader.
			{"tsc_class_board", 0, "tsc:3:2:1", "metal", null},
			{"tsc_video_wall", 0, "tsc:2:2:3", "metal", null},
			{"tsc_console", 0, "tsc:3:1:7", "metal", null},
			{"tsc_comms_console", 0, "tsc:3:1:1", "metal", null},
			{"tsc_nrc_desk", 0, "tsc:3:1:1", "metal", null},
			{"tsc_status_board", 0, "tsc:3:2:4", "metal", null},
			{"tsc_area_monitor", 0, "tsc:1:2:1", "metal", null},
			{"tsc_frisker", 0, "tsc:1:2:1", "metal", null},
			{"tsc_procedure_shelf", 0, "plain", "metal", new double[] {0.6, 0, 2.0, 15.4, 16, 14.8}},
			{"tsc_plan_file", 0, "plain", "metal", new double[] {0.4, 0, 1.0, 15.6, 13.6, 15.6}},
			{"tsc_card_reader", 0, "plain", "metal", new double[] {5.6, 6.0, 0.0, 10.4, 13.4, 1.0}},
			// The operations office (round 26, #13: OpsOffice, tools/ops_office.py): its out-of-service board, the work control
			// consoles, the shift schedule board, the master clock, the radio charging rack, the PPE shelf (live:
			// ops:width:rows:variants), the locked valve key locker, the clearance tag rack, the group lockbox, the procedures.
			{"ops_oos_board", 0, "ops:3:2:1", "metal", null},
			{"ops_console", 0, "ops:3:1:2", "metal", null},
			{"ops_schedule_board", 0, "ops:3:2:1", "metal", null},
			{"ops_wall_clock", 0, "ops:1:1:1", "metal", null},
			{"ops_radio_rack", 0, "ops:1:1:1", "metal", null},
			{"ops_ppe_shelf", 0, "ops:2:1:1", "metal", null},
			{"ops_key_locker", 0, "plain", "metal", new double[] {1.0, 1.6, 0.0, 15.0, 14.6, 3.7}},
			{"ops_tag_rack", 0, "plain", "metal", new double[] {1.0, 1.0, 0.0, 15.0, 15.0, 2.0}},
			{"ops_lockbox", 0, "plain", "metal", new double[] {2.0, 1.2, 0.0, 14.0, 13.0, 5.5}},
			{"ops_procedure_shelf", 0, "plain", "metal", new double[] {0.6, 0, 2.0, 15.4, 16, 14.8}},
			// The restroom's plain pieces (round 26, #14; tools/restroom.py; the fixtures are RestroomBlocks).
			{"restroom_mirror", 0, "plain", "metal", new double[] {1.0, 0.4, 0.0, 15.0, 14.6, 0.6}},
			{"restroom_towel_dispenser", 0, "plain", "metal", new double[] {5.4, 0.9, 0.0, 10.6, 9.0, 1.9}},
			{"restroom_stall_panel", 0, "rrtall", "metal", new double[] {0, 0, 0, 16, 24, 1.1}},
			{"restroom_urinal_screen", 0, "rrtall", "metal", new double[] {0, 5.6, 0, 1.1, 22.6, 7.4}},
			{"restroom_changing_station", 0, "plain", "metal", new double[] {1.2, 13.0, 0, 14.8, 16, 2.4}},
			{"restroom_floor_drain", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"restroom_exhaust_grille", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"sign_wash_hands", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"restroom_stall_partition", 0, "rrpart:5", "metal", new double[] {0, 0, 0.3, 16, 24, 0.8}},
			{"restroom_stall_headrail", 0, "rrpart:3", "metal", new double[] {0, 0, 0, 16, 1.0, 0.75}},
			// The cable spreading room (punch list #15; tools/spreading_room.py, SpreadingRoomFire): the trays by tier, their
			// bundles and risers (train:n: the variant is the safety train, and for the trays with a hanger, TrainFurnitureBlock),
			// the hanger rods, the gaps' barrier boards, the relay header's tee; Train B's fire-wrapped raceway; the pre-action
			// sprinkler system: branch lines, heads, the cross main, the riser, its valve station (live), its releasing panel
			// (CsrReleasePanelBlock) and pull station (FirePullStationBlock); the heat detectors and the room's signs.
			{"cable_tray_power", 0, "train:6", "metal", new double[] {0, 11.2, 3, 16, 14, 13}},
			{"cable_tray_control", 0, "train:6", "metal", new double[] {0, 11.2, 3, 16, 14, 13}},
			{"cable_tray_instr", 0, "train:6", "metal", new double[] {0, 11.2, 3, 16, 14, 13}},
			{"cable_tray_power_drop", 0, "train:6", "metal", new double[] {0, 11.2, 3, 16, 16, 13}},
			{"cable_tray_control_drop", 0, "train:6", "metal", new double[] {0, 11.2, 3, 16, 16, 13}},
			{"cable_tray_instr_drop", 0, "train:6", "metal", new double[] {0, 11.2, 3, 16, 16, 13}},
			{"cable_tray_power_up", 0, "train:3", "metal", new double[] {0, 11.2, 3, 16, 16, 13}},
			{"cable_bundle", 0, "train:3", "metal", new double[] {6.5, 0, 6.5, 9.5, 16, 9.5}},
			{"cable_bundle_elbow", 0, "train:3", "metal", new double[] {6.5, 13.8, 0, 9.5, 16, 9.5}},
			{"cable_bundle_sleeve", 0, "train:3", "metal", new double[] {5.2, 0, 5.2, 10.8, 16, 10.8}},
			{"cable_tray_riser", 0, "train:3", "metal", new double[] {3, 0, 0, 13, 16, 3.4}},
			{"cable_tray_riser_floor", 0, "train:3", "metal", new double[] {3, 0, 0, 13, 16, 3.4}},
			{"cable_tray_riser_top", 0, "train:3", "metal", new double[] {3, 0, 0, 13, 16, 16}},
			{"cable_tray_rods", 0, "plain", "metal", new double[] {2.6, 0, 1.6, 3.4, 16, 14.4}},
			{"cable_tray_gap", 0, "train:2", "metal", new double[] {7.3, 0, 1, 8.7, 16, 15}},
			{"cable_tray_control_tee", 0, "plain", "metal", new double[] {0, 11.2, 3, 16, 14, 16}},
			{"fire_wrap", 0, "plain", "metal", new double[] {0, 10.5, 2.4, 16, 14.8, 13.6}},
			{"fire_wrap_start", 0, "plain", "metal", new double[] {0, 10.5, 0, 14, 16, 13.6}},
			{"fire_wrap_up", 0, "plain", "metal", new double[] {2.4, 10.5, 2.4, 16, 16, 13.6}},
			{"fire_wrap_tee_up", 0, "plain", "metal", new double[] {0, 10.5, 2.4, 16, 16, 13.6}},
			{"fire_wrap_rise", 0, "plain", "metal", new double[] {5.4, 0, 5.4, 10.6, 16, 10.6}},
			{"fire_wrap_sleeve", 0, "plain", "metal", new double[] {5.4, 0, 5.4, 10.6, 16, 10.6}},
			{"sprinkler_pipe", 0, "plain", "metal", new double[] {0, 13.4, 7.4, 16, 14.6, 8.6}},
			{"sprinkler_pipe_head", 0, "plain", "metal", new double[] {0, 10.2, 7, 16, 16, 9}},
			{"sprinkler_pipe_end", 0, "plain", "metal", new double[] {0, 10.2, 7, 10.2, 16, 9}},
			{"sprinkler_main", 0, "plain", "metal", new double[] {7.1, 13.1, 0, 8.9, 16, 16}},
			{"sprinkler_main_cross", 0, "plain", "metal", new double[] {0, 13.1, 0, 16, 16, 16}},
			{"sprinkler_main_end", 0, "plain", "metal", new double[] {0, 13.1, 6, 16, 16, 16}},
			{"sprinkler_riser", 0, "plain", "metal", new double[] {7, 0, 11, 9, 16, 16}},
			{"sprinkler_riser_top", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"heat_detector", 0, "plain", "metal", new double[] {5.6, 13.4, 5.6, 10.4, 16, 10.4}},
			{"fire_pull_station", 0, "pullstation", "metal", new double[] {4.4, 2, 0, 11.6, 11.6, 3.6}},
			{"csr_valve_station", 0, "wall:3:10.5", "metal", null},
			{"csr_release_panel", 0, "csrpanel", "metal", null},
			{"sign_csr_train_a", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_csr_train_b", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_csr_train_n", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_csr_preaction", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The switchgear room (older rooms round 1; tools/switchgear_room.py, LoadCentre, SwitchgearRoom): the overhead power
			// trays over the load centres (train:n as the spreading room's), their drops into the gland plates and their ends up
			// through the ceiling; LC 1A's fire-wrapped way up through the spreading room; the room's fan coil unit and supply
			// duct; the breaker handling gear; the log desk (the laptop end live: SgrLaptopBlock); the one-line diagram; signs.
			{"cable_tray_power_down", 0, "train:6", "metal", new double[] {0, 0, 1.4, 16, 16, 14.6}},
			{"lc_cable_drop", 0, "train:3", "metal", new double[] {6.3, 0, 6.3, 9.7, 16, 9.7}},
			{"cable_tray_power_ceiling", 0, "train:3", "metal", new double[] {2.8, 11.2, 2.2, 16, 16, 14}},
			{"fire_wrap_floor", 0, "plain", "metal", new double[] {4, 0, 4, 12, 16, 12}},
			{"fire_wrap_elbow", 0, "plain", "metal", new double[] {0, 0, 2.4, 10.7, 14.8, 13.6}},
			{"fire_wrap_hanger", 0, "plain", "metal", new double[] {0, 9.6, 1.4, 16, 16, 14.6}},
			{"fire_wrap_drop", 0, "plain", "metal", new double[] {4.7, 0, 2.3, 16, 14.9, 16}},
			{"sgr_duct", 0, "plain", "metal", new double[] {0.9, 0.9, 0, 15.1, 15.1, 16}},
			{"sgr_duct_hanger", 0, "plain", "metal", new double[] {0.9, 0.9, 0, 15.1, 16, 16}},
			{"sgr_duct_register", 0, "plain", "metal", new double[] {0.9, 0.9, 0, 15.1, 15.1, 16}},
			{"sgr_duct_end", 0, "plain", "metal", new double[] {0.9, 0.9, 0, 15.1, 15.1, 16}},
			{"sgr_fan_coil", 0, "plain", "metal", new double[] {0.6, 0.9, 0, 16, 16, 16}},
			{"sgr_fan_coil_end", 0, "plain", "metal", new double[] {0, 0.2, 1, 16, 16, 15.4}},
			{"breaker_lifter", 0, "plain", "metal", new double[] {1, 0, 1, 16, 16, 15.6}},
			{"breaker_lifter_top", 0, "plain", "metal", new double[] {3, 0, 0.2, 16, 15.6, 3.6}},
			{"remote_racking_device", 0, "plain", "metal", new double[] {1.2, 0, 1.2, 14.8, 16, 15.8}},
			{"breaker_test_set", 0, "plain", "metal", new double[] {0.6, 0, 1.8, 15, 16, 14.4}},
			{"sgr_log_desk", 0, "variant:2", "metal", new double[] {0, 0, 1, 16, 16, 14.6}},
			{"sgr_desk_laptop", 0, "sgrlaptop", "metal", null},
			{"sgr_one_line", 0, "multi:3:2:v", "wood", null},
			{"sign_lc_a", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_lc_b", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_switchgear_danger", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The turbine building's switchgear room (turbine hall part 3): LC 1C's and 1D's signs, its one-line diagram, the
			// trays' hanger rods on up through the tall room.
			{"sign_lc_c", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_lc_d", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sgr_one_line_tb", 0, "multi:3:2:v", "wood", null},
			{"cable_tray_rod_run", 0, "plain", "metal", new double[] {2.6, 0, 1.6, 3.4, 16, 14.4}},
			// The plant computer room (older rooms round 2; tools/server_room.py, ServerRoom): the racks (their doors open on
			// a click), the UPS lineup (the module the room's hub), the CRACs, the clean agent cylinders, releasing panel, horn
			// and strobe, the console (live: srv:width:rows:variants); the abort and manual release stations; the agent pipe,
			// the cable ladders and fiber runway, the signs.
			{"server_rack", 0, "srv:1:2:1", "metal", null},
			{"network_rack", 0, "srv:1:2:1", "metal", null},
			{"san_rack", 0, "srv:1:2:1", "metal", null},
			{"ppc_rack", 0, "srv:1:2:1", "metal", null},
			{"ups_module", 0, "srv:1:2:1", "metal", null},
			{"ups_battery", 0, "srv:1:2:1", "metal", null},
			{"ups_bypass", 0, "srv:1:2:1", "metal", null},
			{"pdu_cabinet", 0, "srv:1:2:1", "metal", null},
			{"crac_unit", 0, "srv:2:2:2", "metal", null},
			{"agent_cylinder", 0, "srv:1:2:1", "metal", null},
			{"agent_release_panel", 0, "srv:1:2:1", "metal", null},
			{"agent_horn_strobe", 0, "srv:1:1:1", "metal", null},
			{"ppc_console", 0, "srv:3:1:1", "metal", null},
			{"agent_abort_station", 0, "srvabort", "metal", new double[] {4.4, 2.0, 0, 11.6, 11.0, 2.9}},
			{"agent_manual_release", 0, "srvpull", "metal", new double[] {4.4, 2.0, 0, 11.6, 11.6, 3.0}},
			{"agent_pipe", 0, "plain", "metal", new double[] {0, 12.0, 7.0, 16, 16, 9.0}},
			{"agent_pipe_nozzle", 0, "variant:2", "metal", new double[] {0, 10.0, 6.9, 16, 16, 9.1}},
			{"agent_pipe_elbow", 0, "plain", "metal", new double[] {0, 12.1, 7.1, 8.9, 13.9, 16}},
			{"agent_pipe_tee", 0, "plain", "metal", new double[] {0, 12.1, 7.1, 16, 13.9, 16}},
			{"agent_pipe_riser", 0, "plain", "metal", new double[] {6.6, 0, 0.2, 9.4, 16, 3.7}},
			{"agent_pipe_riser_top", 0, "plain", "metal", new double[] {6.6, 0, 0.2, 9.4, 13.9, 16}},
			{"agent_manifold", 0, "variant:3", "metal", new double[] {0, 0, 0.2, 16, 16, 8.0}},
			{"srv_ladder", 0, "variant:8", "metal", new double[] {0, 5.5, 0.5, 16, 16, 15}},
			{"srv_ladder_cross", 0, "variant:2", "metal", new double[] {0, 5.5, 0.8, 16, 7.6, 6.6}},
			{"srv_ladder_riser", 0, "variant:3", "metal", new double[] {0.4, 0, 0.2, 7.2, 16, 3.0}},
			{"sign_srv_row_a", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_srv_row_b", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_srv_row_c", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_srv_row_d", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_no_food", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_clean_agent", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			{"sign_agent_vacate", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The turbine hall part 3. The workshop (tools/shop_room.py): its bench, pegboard, lathe and gantry each in three
			// set-ups (cycle:n: CycleFurnitureBlock), the drill press, tool chest, shop press, parts washer, the welding
			// booth's screen, table, welder and torch cart, the eye protection sign.
			{"shop_bench", 0, "cycle:3", "metal", new double[] {0, 0, 3, 16, 16, 16}},
			{"shop_pegboard", 0, "cycle:3", "wood", new double[] {0, 0, 0, 16, 16, 4.5}},
			{"shop_lathe", 0, "cycle:3", "metal", new double[] {0, 0, 1.8, 16, 16, 16}},
			{"shop_gantry", 0, "cycle:3", "metal", new double[] {0, 0, 0.4, 16, 16, 15.6}},
			{"shop_drill_press", 0, "plain", "metal", new double[] {1.2, 0, 0.2, 13.4, 16, 14.6}},
			{"shop_tool_chest", 0, "plain", "metal", new double[] {1.4, 0, 2.8, 15.2, 16, 13.2}},
			{"shop_press", 0, "plain", "metal", new double[] {0.8, 0, 2, 16, 16, 14}},
			{"shop_parts_washer", 0, "plain", "metal", new double[] {1.4, 0, 1.6, 14.6, 16, 14}},
			{"shop_weld_screen", 0, "plain", "metal", new double[] {0, 0, 4, 16, 16, 12}},
			{"shop_weld_table", 0, "plain", "metal", new double[] {0.6, 0, 1, 15.8, 16, 15.4}},
			{"shop_welder", 0, "plain", "metal", new double[] {1.9, 0, 0.6, 14.8, 16, 14}},
			{"shop_torch_cart", 0, "plain", "metal", new double[] {0.3, 0, 2.2, 15.7, 16, 13.6}},
			{"sign_eye_protection", 0, "plain", "metal", new double[] {2.5, 1.5, 0, 13.5, 14.5, 0.5}},
			// The turbine control room (tools/tcr_room.py, TurbineControlRoom: TcrPanelBlock, tcr:width:labels).
			{"tb_tcs_power", 0, "tcr:1", "metal", null},
			{"tb_tcs_control", 0, "tcr:1", "metal", null},
			{"tb_tcs_io", 0, "tcr:1:I/O CAB 1,I/O CAB 2", "metal", null},
			{"tb_tsi_rack", 0, "tcr:1", "metal", null},
			{"tb_excitation", 0, "tcr:2", "metal", null},
			{"tb_gen_protection", 0, "tcr:1:SYSTEM A,SYSTEM B", "metal", null},
			// The hall's fittings (tools/hall_fittings.py): the crane pendant stations (CranePendantBlock: HallCrane), the
			// crane's travel direction signs (LabelledSignBlock), the fire hose reels, the plant page stations and horns,
			// the floor markings (cycle:15: the yellow lines and the laydown bay's stencils), the lube oil room's spill kit.
			{"tb_crane_pendant", 0, "cranependant", "metal", null},
			{"tb_crane_compass", 0, "signs:2:2:N,E,S,W", "metal", null},
			{"tb_hose_reel", 0, "plain", "metal", new double[] {1.05, 0, 0, 14.2, 16, 10.3}},
			{"tb_page_station", 0, "plain", "metal", new double[] {4, 7.8, 0, 12, 16, 6.6}},
			{"tb_page_horn", 0, "plain", "metal", new double[] {4.6, 0, 0, 11.4, 16, 8.4}},
			{"tb_floor_mark", 0, "cycle:15", "metal", new double[] {0, 0, 0, 16, 0.1, 16}},
			{"spill_kit_oil", 0, "plain", "metal", new double[] {1.7, 1, 0, 14.3, 14.5, 6.6}},
	};
	/**
	 * Plain full blocks: suspended ceiling, floors, walls, tank plate, louvres; and the control room's floor over the cable
	 * spreading room (its carpet or access floor on top, the bare slab's painted underside below: punch list #15).
	 */
	public static final String[] CUBES = {"ceiling_tile", "vinyl_floor", "epoxy_floor", "painted_wall", "tank_plate", "intake_louvre",
			"carpet_floor_slab", "access_floor_slab", "access_floor_perforated"};

	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static Block ROLLER_DOOR;
	public static Block BUILDING_SIGN;
	public static Block ROOM_SIGN;
	public static BlockEntityType<PlantSignBlockEntity> SIGN_ENTITY;
	public static BlockEntityType<StatusScreenBlockEntity> STATUS_ENTITY;
	/** The office computers' live screens (the administration building part 1). */
	public static BlockEntityType<DeskScreenBlockEntity> DESK_ENTITY;
	/** The unit performance screens' (the administration building part 2: PerfScreenBlockEntity, at the bottom middle). */
	public static BlockEntityType<PerfScreenBlockEntity> PERF_ENTITY;
	/** Round 34: the plant overview screen's (OverviewScreenBlockEntity, at its bottom middle block). */
	public static BlockEntityType<OverviewScreenBlockEntity> OVERVIEW_ENTITY;
	public static BlockEntityType<LivePanelBlockEntity> LIVE_PANEL_ENTITY;
	public static BlockEntityType<StationBatteryBlockEntity> STATION_BATTERY_ENTITY;
	public static BlockEntityType<LockerBlockEntity> LOCKER_ENTITY;
	public static BlockEntityType<StorageRackBlockEntity> RACK_ENTITY;
	public static BlockEntityType<RollerDoorBlockEntity> ROLLER_ENTITY;
	public static BlockEntityType<SpeedGateBlockEntity> SPEED_GATE_ENTITY;
	public static Item LAB_COAT, SECURITY_JACKET;

	public static Item uniform(UniformItem.Kind kind) {
		return kind == UniformItem.Kind.LAB_COAT ? LAB_COAT : SECURITY_JACKET;
	}

	private static Item uniformItem(String name, UniformItem.Kind kind) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new UniformItem(new Item.Properties().setId(key).stacksTo(1)
				.component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
						net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
								.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
								.setAsset(ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Jugcraft.id("uniform")))
								.build()), kind));
	}

	/** Doors that open by hand, with steel door sounds. */
	public static final BlockSetType STEEL = new BlockSetType("jugcraft_steel", true, true, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
			SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
			SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF,
			SoundEvents.STONE_BUTTON_CLICK_ON);

	private PlantInterior() {
	}

	public static void register() {
		LAB_COAT = uniformItem("lab_coat", UniformItem.Kind.LAB_COAT);
		SECURITY_JACKET = uniformItem("security_jacket", UniformItem.Kind.SECURITY_JACKET);
		for (Object[] f : FURNITURE) {
			String id = (String) f[0];
			int light = (Integer) f[1];
			String kind = (String) f[2];
			double[] shape = (double[]) f[4];
			Block like = switch ((String) f[3]) {
				case "wood" -> Blocks.OAK_PLANKS;
				case "wool" -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.GRAY);
				case "plant" -> Blocks.AZALEA;
				default -> Blocks.IRON_BLOCK;
			};
			BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(like).strength(1.5F).noOcclusion().lightLevel(state -> light);
			block(id, p -> {
				String[] k = kind.split(":");
				if (kind.equals("stack")) {
					return new StackingFurnitureBlock(p, shape);
				} else if (kind.equals("high_bay")) {
					return new HighBayLightBlock(p, shape);
				} else if (kind.equals("rack")) {
					return new StorageRackBlock(p);
				} else if (k[0].equals("locker")) {
					return new LockerBlock(p, k[1].equals("lab") ? UniformItem.Kind.LAB_COAT : UniformItem.Kind.SECURITY_JACKET, shape);
				} else if (k[0].equals("stack")) {
					return new StackingFurnitureBlock(p, shape).group(k[1]);
				} else if (kind.equals("status")) {
					return new StatusScreenBlock(p);
				} else if (kind.equals("overview")) {
					return new OverviewScreenBlock(p);
				} else if (kind.equals("vital")) {
					return new VitalPanelBlock(p);
				} else if (kind.equals("dcpanel")) {
					return new DcPanelBlock(p);
				} else if (kind.equals("dcboard")) {
					return new DcSwitchboardBlock(p);
				} else if (kind.equals("dccharger")) {
					return new StationChargerBlock(p);
				} else if (kind.equals("dcinv")) {
					return new VitalInverterBlock(p);
				} else if (kind.equals("dclaptop")) {
					return new DcLaptopBlock(p);
				} else if (kind.equals("bdisc")) {
					return new BatteryDisconnectBlock(p);
				} else if (k[0].equals("crcon")) {
					return new CrConsoleBlock(p, Integer.parseInt(k[1]));
				} else if (kind.equals("acpost")) {
					return new AcStationBlock(p);
				} else if (kind.equals("secsas")) {
					return new SecAlarmConsoleBlock(p);
				} else if (kind.equals("secxray")) {
					return new SecXrayBlock(p);
				} else if (kind.equals("secwtmd")) {
					return new MetalDetectorBlock(p);
				} else if (kind.equals("achand")) {
					return new HandReaderBlock(p, shape);
				} else if (kind.equals("ground")) {
					return new GroundDetectorBlock(p);
				} else if (kind.equals("fan")) {
					return new ExhaustFanBlock(p);
				} else if (kind.equals("eyewash")) {
					return new EyewashBlock(p, shape);
				} else if (k[0].equals("relay")) {
					return new RelayCabinetBlock(p, Integer.parseInt(k[1]));
				} else if (kind.equals("hvpanel")) {
					return new HvacPanelBlock(p);
				} else if (k[0].equals("hvmon")) {
					return new HvacMonitorBlock(p, Integer.parseInt(k[1]), Double.parseDouble(k[2]));
				} else if (kind.equals("damper")) {
					return DamperBlock.create(p, shape);
				} else if (k[0].equals("tsc")) {
					return new TscPanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), Integer.parseInt(k[3]));
				} else if (kind.equals("rrtall")) {
					return new RestroomBlocks.Tall(p, shape);
				} else if (k[0].equals("rrpart")) {
					return RestroomBlocks.Part.create(p, Integer.parseInt(k[1]), shape);
				} else if (k[0].equals("ops")) {
					return new OpsPanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), Integer.parseInt(k[3]));
				} else if (k[0].equals("wall")) {
					return new WallLivePanelBlock(p, Integer.parseInt(k[1]), Double.parseDouble(k[2]));
				} else if (k[0].equals("wallw")) {
					return new WallLivePanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), Double.parseDouble(k[3]));
				} else if (kind.equals("co2station")) {
					return new Co2StationBlock(p);
				} else if (kind.equals("csrpanel")) {
					return new CsrReleasePanelBlock(p);
				} else if (kind.equals("sgrlaptop")) {
					return new SgrLaptopBlock(p);
				} else if (k[0].equals("srv")) {
					return new ServerRoomPanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), Integer.parseInt(k[3]));
				} else if (kind.equals("srvabort") || kind.equals("srvpull")) {
					return new AgentStationBlock(p, kind.equals("srvabort"), shape);
				} else if (kind.equals("pullstation")) {
					return new FirePullStationBlock(p, shape);
				} else if (k[0].equals("train")) {
					return TrainFurnitureBlock.create(p, Integer.parseInt(k[1]), shape);
				} else if (kind.equals("pendant")) {
					return new HoistPendantBlock(p);
				} else if (kind.equals("cranependant")) {
					return new CranePendantBlock(p);
				} else if (k[0].equals("tcr")) {
					return new TcrPanelBlock(p, Integer.parseInt(k[1]), k.length > 2 ? kind.substring(kind.indexOf(':', 4) + 1).split(",") : new String[0]);
				} else if (k[0].equals("signs")) {
					return new LabelledSignBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), k[3].split(","));
				} else if (k[0].equals("cycle")) {
					return CycleFurnitureBlock.create(p, Integer.parseInt(k[1]), shape);
				} else if (k[0].equals("edgpanel")) {
					return new EdgPanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]));
				} else if (k[0].equals("live")) {
					return new LivePanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), k.length > 4 ? Integer.parseInt(k[4]) : 1);
				} else if (k[0].equals("battery")) {
					// A station battery that holds charge (round 25).
					return new StationBatteryBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), k.length > 4 ? Integer.parseInt(k[4]) : 1);
				} else if (k[0].equals("multi")) {
					// "b": a machine standing on the floor (rising a little into the row above); "p": a panel standing on the
					// floor, full height in both rows.
					return new MultiFurnitureBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), !k[3].equals("h") && !k[3].equals("c"),
							k.length > 4 ? Integer.parseInt(k[4]) : 1, k[3].equals("b") || k[3].equals("p"), k[3].equals("c"),
							k[3].equals("p") ? 15.6 : 7);
				} else if (k[0].equals("steam")) {
					return new SteamFurnitureBlock(p, Double.parseDouble(k[1]), Double.parseDouble(k[2]), Double.parseDouble(k[3]), shape);
				} else if (k[0].equals("seat")) {
					return new SeatFurnitureBlock(p, Double.parseDouble(k[1]), shape);
				} else if (k[0].equals("deskrow")) {
					return DeskComputerRowBlock.create(p, k[1], Double.parseDouble(k[2]), Integer.parseInt(k[3]),
							java.util.Arrays.stream(k[4].split(",")).mapToInt(Integer::parseInt).toArray(), shape);
				} else if (kind.equals("deskpc")) {
					return new DeskComputerBlock(p, shape);
				} else if (kind.equals("pull")) {
					return new FireAlarmPullBlock(p, shape);
				} else if (kind.equals("chemboard")) {
					return new ChemBoardBlock(p);
				} else if (kind.equals("gammaspec")) {
					return new GammaSpecBlock(p);
				} else if (kind.equals("safetycab")) {
					return new SafetyCabinetBlock(p);
				} else if (kind.equals("shower")) {
					return new SafetyShowerBlock(p);
				} else if (kind.equals("perf")) {
					return new PerfScreenBlock(p);
				} else if (kind.equals("hotline")) {
					return new HotlineBlock(p);
				} else if (k[0].equals("row")) {
					return RowFurnitureBlock.create(p, k[1], k.length > 2 ? Double.parseDouble(k[2]) : 0, k.length > 3 ? Integer.parseInt(k[3]) : 0, shape);
				} else if (kind.startsWith("variant:")) {
					return VariantFurnitureBlock.create(p, Integer.parseInt(kind.substring(8)), shape);
				}
				return new FurnitureBlock(p, shape);
			}, props);
		}
		// The flat screen TV (tv/FlatTvBlock.java): blocks that join into one screen up to 8 x 5, playing a video link for
		// everyone near it (once a 3 x 2 piece, the "multi" furniture).
		Block tv = block("flat_tv", io.github.jimbozoomer.jugcraft.tv.FlatTvBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(1.5F).noOcclusion().sound(SoundType.METAL));
		io.github.jimbozoomer.jugcraft.tv.JugcraftTv.register(tv);
		// The restroom's fixtures (round 26, #14): toilets, urinal, sinks, hand dryer, trash can, stall doors, tile.
		RestroomBlocks.register();
		// Document control's shelving (the administration building part 2): the mobile carriages, the fixed units at a
		// bank's ends, the floor rail; the plan file. The carriages roll each level tick (MobileShelvingBlock.tick).
		BlockBehaviour.Properties shelving = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion();
		block("doc_mobile_shelving", MobileShelvingBlock::new, shelving);
		block("doc_fixed_shelving", ShelvingBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion());
		block("doc_plan_file", PlanFileBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion());
		block("doc_rail_floor", RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.5F, 6F));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(MobileShelvingBlock::tick);
		// The desk with a coffee on it steams (its mug: tools/plant_office2.py MUG_AT).
		((RowFurnitureBlock) BLOCKS.get("office_desk_long")).steam(2, 11.8, 15.6, 7.8);
		((VariantFurnitureBlock) BLOCKS.get("cafe_table")).steam(5, 11.7, 15.3, 6.2);
		block("conference_table", ConferenceTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion());
		for (String id : CUBES) {
			BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(id.contains("tank") || id.contains("louvre")
					? Blocks.IRON_BLOCK : Blocks.STONE).strength(1.5F, 6F);
			block(id, Block::new, props);
		}
		block("hvac_duct", RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6F));
		for (String id : new String[] {"steel_door", "glass_door"}) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
			// A swinging door with a closer (doors round: PlantDoorBlock; the glass door's closer is in its head).
			Block door = Registry.register(BuiltInRegistries.BLOCK, key, new PlantDoorBlock(STEEL, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR)
					.strength(3.0F).noOcclusion().setId(key), true));
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new DoubleHighBlockItem(door, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
			BLOCKS.put(id, door);
		}
		// The speed gate: its lane block (the item) and its pedestals (no item: they come with the gates).
		block("speed_gate", SpeedGateBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion()
				.sound(net.minecraft.world.level.block.SoundType.METAL));
		ResourceKey<Block> pedestalKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("speed_gate_pedestal"));
		BLOCKS.put("speed_gate_pedestal", Registry.register(BuiltInRegistries.BLOCK, pedestalKey, new SpeedGatePedestalBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion().setId(pedestalKey))));
		SPEED_GATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("speed_gate"),
				FabricBlockEntityTypeBuilder.create(SpeedGateBlockEntity::new, BLOCKS.get("speed_gate")).build());
		// Framed glass: a storefront partition that meets the glass doors flush (FramedGlassBlock).
		block("framed_glass", FramedGlassBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE).strength(1.0F, 6F).noOcclusion());
		ROLLER_DOOR = block("roller_door", RollerDoorBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion());
		// Its push-button station on the wall beside the opening (doors round).
		block("roller_door_station", RollerDoorStationBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6F)
				.noOcclusion());
		BUILDING_SIGN = block("building_sign", PlantSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion()
				.noCollision());
		ROOM_SIGN = block("room_sign", PlantSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(0.5F).noOcclusion()
				.noCollision());
		SIGN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("plant_sign"),
				FabricBlockEntityTypeBuilder.create(PlantSignBlockEntity::new, BUILDING_SIGN, ROOM_SIGN).build());
		STATUS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("status_screen"),
				FabricBlockEntityTypeBuilder.create(StatusScreenBlockEntity::new, BLOCKS.get("status_screen")).build());
		DESK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("desk_screen"),
				FabricBlockEntityTypeBuilder.create(DeskScreenBlockEntity::new, BLOCKS.get("office_desk"), BLOCKS.get("office_desk_long"),
						BLOCKS.get("executive_desk"), BLOCKS.get("glass_desk"), BLOCKS.get("reception_desk"), BLOCKS.get("doc_issue_counter")).build());
		PERF_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("perf_screen"),
				FabricBlockEntityTypeBuilder.create(PerfScreenBlockEntity::new, BLOCKS.get("perf_screen")).build());
		OVERVIEW_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("plant_overview_screen"),
				FabricBlockEntityTypeBuilder.create(OverviewScreenBlockEntity::new, BLOCKS.get("plant_map_screen")).build());
		LIVE_PANEL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("live_panel"),
				FabricBlockEntityTypeBuilder.create(LivePanelBlockEntity::new, BLOCKS.entrySet().stream()
						.filter(e -> e.getValue() instanceof LivePanelBlock).map(java.util.Map.Entry::getValue).toArray(Block[]::new)).build());
		STATION_BATTERY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("station_battery"),
				FabricBlockEntityTypeBuilder.create(StationBatteryBlockEntity::new, BLOCKS.get("station_battery")).build());
		// The DC system holds charge (round 25): a cable into a station battery charges it or draws on it; a cable into a
		// battery charger is its AC supply.
		io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
				StationBatteryBlock.storage(level, pos), BLOCKS.get("station_battery"));
		io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
				LivePanelBlockEntity.chargerInput(level, pos), BLOCKS.get("edg_battery_charger"), BLOCKS.get("station_charger"));
		RACK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("storage_rack"),
				FabricBlockEntityTypeBuilder.create(StorageRackBlockEntity::new, BLOCKS.get("storage_rack")).build());
		ROLLER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("roller_door"),
				FabricBlockEntityTypeBuilder.create(RollerDoorBlockEntity::new, ROLLER_DOOR).build());
		LOCKER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("locker"),
				FabricBlockEntityTypeBuilder.create(LockerBlockEntity::new, BLOCKS.get("locker_open"), BLOCKS.get("locker_security")).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(LAB_COAT);
			output.accept(SECURITY_JACKET);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BLOCKS.values().stream()
				.filter(b -> b.asItem() != net.minecraft.world.item.Items.AIR).forEach(output::accept));
	}

	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		BLOCKS.put(name, block);
		return block;
	}
}
