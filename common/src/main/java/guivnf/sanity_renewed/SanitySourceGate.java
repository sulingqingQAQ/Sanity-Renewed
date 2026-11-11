package guivnf.sanity_renewed;

/**
 * Central switches of the Depression addon, phase 1 ("independent sanity slice").
 *
 * <p>Every legacy way of changing sanity that the original mod had is closed here, in one place,
 * instead of being removed from a dozen mixins and listeners. The checks sit at the choke points
 * ({@link SanityProcessor#addSanity}, the {@code SanityProcessor.handle*} entry points, the passive
 * loop in {@link SanityProcessor#tickPlayer} and the inner entity spawner), so a source that is wired
 * up elsewhere still cannot write.
 *
 * <p>These are compile-time constants on purpose: phase 1 ships with them closed and a later phase
 * opens them one by one in code. The debug commands ({@code /sanity set|add}) and the two phase-1
 * recovery sources ({@code SanityRecovery}) do not go through these gates.
 */
public final class SanitySourceGate
{
    /**
     * The 15 original passive sources (rain, hunger, darkness, monsters, pets, jukebox, dirt path,
     * player company, campfire-as-passive-block, thirst/temperature, garland, ...). Closed.
     * The campfire effect is re-implemented as its own source in {@code SanityRecovery}.
     */
    public static final boolean LEGACY_PASSIVE_SOURCES = false;

    /**
     * The original one-shot sources (sleeping, hurt, pet death, advancement, breeding, trading, shearing,
     * eating, fishing, potting, egg, farmland, dimension change, lightning, item and block lists,
     * inner entity kill reward). Closed.
     */
    public static final boolean LEGACY_ACTIVE_SOURCES = false;

    /**
     * Compile-time phase gate for inner entity spawning. Phase 1 shipped with {@code false}.
     * Phase 3 opens this gate, but spawning is still OFF by default at runtime: it additionally
     * requires the explicit operator switch {@code sanity.phase3.inner_entity_spawning = true}
     * (see {@link guivnf.sanity_renewed.entity.Phase3Features}), so a fresh install keeps the exact
     * phase-1 behaviour (no new inner entities) until an operator opts in.
     */
    public static final boolean INNER_ENTITY_SPAWNING = true;

    private SanitySourceGate() {}
}
