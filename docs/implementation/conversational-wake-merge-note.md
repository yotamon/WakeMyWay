# Conversational Wake merge note

This branch is intended to merge as a founder/debug conversational-wake capability on top of the stable permission-gated alarm baseline from PR #38.

The critical alarm path remains local and unchanged. Realtime WebRTC exists only in the debug source set, and local Alfred remains the fallback.