package com.nowayup.system;

import java.util.Map;
import net.minecraft.network.chat.Component;

public final class NoWayUpText {
    private static final Map<String, String> SIGN_KEYS = Map.ofEntries(
        Map.entry("Do not", "nowayup.sign.do_not"),
        Map.entry("climb this", "nowayup.sign.climb_this"),
        Map.entry("time.", "nowayup.sign.time"),
        Map.entry("Crouch here", "nowayup.sign.crouch_here"),
        Map.entry("when it", "nowayup.sign.when_it"),
        Map.entry("starts to", "nowayup.sign.starts_to"),
        Map.entry("fall.", "nowayup.sign.fall"),
        Map.entry("You are", "nowayup.sign.you_are"),
        Map.entry("early this", "nowayup.sign.early_this"),
        Map.entry("Now", "nowayup.sign.now"),
        Map.entry("watch.", "nowayup.sign.watch"),
        Map.entry("That was", "nowayup.sign.that_was"),
        Map.entry("not the", "nowayup.sign.not_the"),
        Map.entry("exit.", "nowayup.sign.exit"),
        Map.entry("Only down", "nowayup.sign.only_down"),
        Map.entry("stays open.", "nowayup.sign.stays_open"),
        Map.entry("DO NOT", "nowayup.sign.do_not_caps"),
        Map.entry("CLIMB.", "nowayup.sign.climb_caps"),
        Map.entry("THE MINE", "nowayup.sign.the_mine"),
        Map.entry("LEARNED SKY", "nowayup.sign.learned_sky"),
        Map.entry("The mine", "nowayup.sign.the_mine_title"),
        Map.entry("cannot", "nowayup.sign.cannot"),
        Map.entry("follow.", "nowayup.sign.follow"),
        Map.entry("Go below.", "nowayup.sign.go_below"),
        Map.entry("The surface", "nowayup.sign.the_surface"),
        Map.entry("was removed.", "nowayup.sign.was_removed"),
        Map.entry("Kneel", "nowayup.sign.kneel"),
        Map.entry("before the", "nowayup.sign.before_the"),
        Map.entry("wrong wall.", "nowayup.sign.wrong_wall"),
        Map.entry("THIS WAY", "nowayup.sign.this_way"),
        Map.entry("UP", "nowayup.sign.up"),
        Map.entry("NO WAY OUT", "nowayup.sign.no_way_out"),
        Map.entry("He slept", "nowayup.sign.he_slept"),
        Map.entry("here after", "nowayup.sign.here_after"),
        Map.entry("he saw", "nowayup.sign.he_saw"),
        Map.entry("himself.", "nowayup.sign.himself"),
        Map.entry("The exit", "nowayup.sign.the_exit"),
        Map.entry("fell upward.", "nowayup.sign.fell_upward"),
        Map.entry("It learned", "nowayup.sign.it_learned"),
        Map.entry("your route.", "nowayup.sign.your_route"),
        Map.entry("It kept", "nowayup.sign.it_kept"),
        Map.entry("your name.", "nowayup.sign.your_name"),
        Map.entry("Both paths", "nowayup.sign.both_paths"),
        Map.entry("go below.", "nowayup.sign.go_below_lower"),
        Map.entry("was here", "nowayup.sign.was_here"),
        Map.entry("first.", "nowayup.sign.first"),
        Map.entry("You found", "nowayup.sign.you_found"),
        Map.entry("a way down.", "nowayup.sign.a_way_down"),
        Map.entry("never", "nowayup.sign.never"),
        Map.entry("allowed.", "nowayup.sign.allowed"),
        Map.entry("You brought", "nowayup.sign.you_brought"),
        Map.entry("their names", "nowayup.sign.their_names"),
        Map.entry("back.", "nowayup.sign.back"),
        Map.entry("Follow", "nowayup.sign.follow_title"),
        Map.entry("your steps", "nowayup.sign.your_steps"),
        Map.entry("backward.", "nowayup.sign.backward"),
        Map.entry("taught it", "nowayup.sign.taught_it"),
        Map.entry("to sleep.", "nowayup.sign.to_sleep"),
        Map.entry("Maps are", "nowayup.sign.maps_are"),
        Map.entry("how it", "nowayup.sign.how_it"),
        Map.entry("learned us.", "nowayup.sign.learned_us"),
        Map.entry("This time,", "nowayup.sign.this_time"),
        Map.entry("do not", "nowayup.sign.do_not_lower"),
        Map.entry("wake up.", "nowayup.sign.wake_up"),
        Map.entry("The lower", "nowayup.sign.the_lower"),
        Map.entry("path", "nowayup.sign.path"),
        Map.entry("remembers", "nowayup.sign.remembers"),
        Map.entry("less.", "nowayup.sign.less"),
        Map.entry("How far", "nowayup.sign.how_far"),
        Map.entry("will you", "nowayup.sign.will_you"),
        Map.entry("time?", "nowayup.sign.time_question")
    );

    private NoWayUpText() {
    }

    public static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Component signLine(String line) {
        String key = SIGN_KEYS.get(line);
        return key == null ? Component.literal(line) : Component.translatable(key);
    }
}
