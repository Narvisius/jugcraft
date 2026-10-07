package io.github.jimbozoomer.jugcraft.party;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: what the Party screen shows about the receiving player. Only names and flags; the
 * client never decides anything and every button runs an ordinary {@code /party} command.
 *
 * @param enabled     parties are turned on on this server
 * @param maxSize     the member limit
 * @param members     the player's party in join order (empty when not in one)
 * @param invitesFrom the leaders of the parties that invited the player, most recent last
 */
public record PartyStatePayload(boolean enabled, int maxSize, List<Member> members, List<String> invitesFrom)
		implements CustomPacketPayload {
	public static final Type<PartyStatePayload> TYPE = new Type<>(Jugcraft.id("party_state"));
	/** More than any party or invite list the server allows. */
	private static final int MAX_ENTRIES = 128;

	/** A party member as the screen shows them. */
	public record Member(String name, boolean leader, boolean online, boolean you) {
		static final StreamCodec<RegistryFriendlyByteBuf, Member> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Member::name,
				ByteBufCodecs.BOOL, Member::leader,
				ByteBufCodecs.BOOL, Member::online,
				ByteBufCodecs.BOOL, Member::you,
				Member::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PartyStatePayload::enabled,
			ByteBufCodecs.VAR_INT, PartyStatePayload::maxSize,
			Member.CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), PartyStatePayload::members,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_ENTRIES)), PartyStatePayload::invitesFrom,
			PartyStatePayload::new);

	/** True when the receiving player leads their party. */
	public boolean youLead() {
		return members.stream().anyMatch(member -> member.you() && member.leader());
	}

	@Override
	public Type<PartyStatePayload> type() {
		return TYPE;
	}
}
