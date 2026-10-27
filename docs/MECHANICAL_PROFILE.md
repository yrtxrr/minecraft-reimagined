# Executed ordinary-server mechanical profile

Executed2026-10-27, Minecraft1.21.1 / NeoForge21.1.264 / Oracle Java21.0.8, Linux amd64, Intel Xeon Platinum8573C host, cgroup4 CPU quota; JVM `-XX:ActiveProcessorCount=4 -Xmx2G`. Shared cloud hardware, no connected clients. Fresh flat worlds, seed12345, view/simulation distance4, vanilla spawn radius4,1089 loaded chunks reported in both runs. Mob spawning/daylight/weather disabled in both disposable fixtures; no existing entities/AI were removed. Empty-world block-entity suspension explicitly reset in both. Each component is one wheel, axial shaft and mill with64wheat. Three increasing-size phases,200tick warmup then600tick measurement each. JFR profile configuration records warmup too; analysis restricts CPU/allocation/GC events to the measured windows and verifies600 custom tick events/phase. Vanilla per-tick ring timings are sampled after tick completion.

Baseline: gameplay commit3660192 plus the same measurement harness. Candidate: that commit plus the mechanical changes in this checkpoint. Candidate-only restart manifest writes occur after measurement. Recreate baseline in a temporary `git archive 3660192` checkout, copy build.gradle and MechanicalProfile.java from checkpoint9e6eb41, omit only the MechanicalRestartProbe calls (start/tick/saveExpected), and use the same fresh-world setup. The probes are not part of measured gameplay or the shipped JAR.

| Components | Baseline mean/p95/p99/max ms | Candidate mean/p95/p99/max ms | Ticks >50ms baseline/candidate | Observed TPS baseline/candidate |
|---|---|---|---|---|
|0|4.747 /7.105 /135.012 /550.892|1.989 /3.807 /27.546 /234.570|9 /3|20.000 /19.998|
|16|6.997 /17.419 /165.476 /444.355|0.760 /2.004 /5.011 /15.178|13 /0|20.010 /20.001|
|64 (192 mechanical blocks)|2.264 /4.615 /19.837 /405.855|2.038 /4.887 /20.868 /171.110|2 /2|20.000 /19.998|

Every mill produced five flour:80 at16 components,320 at64, in both runs. Reports complete:true. TPS is600 ticks/wall time, including scheduler catch-up, hence it can slightly exceed20; it does not hide the measured long ticks above. These are single short increasing-size runs, not statistically established speedups. Warmup/JIT, shared host variation and asynchronous world saves contribute noise. In particular candidate p99 at64 is slightly higher. No claim of lag-free gameplay or improvement at all scales is supported.

At64 components JFR sampled allocation weights were40.94MiB baseline and59.33MiB candidate per measured30s. These are weighted samples, not exact allocated bytes. Both include about27MiB attributed to vanilla PalettedContainer.pack; candidate also samples16.58MiB in IdentityHashMap iteration. CPU samples mostly show vanilla chunk lookup/unload/save paths; mechanical methods appear sparsely in both (baseline inspect/powered, candidate scan/cache retirement/redstone checks). Too few samples to attribute a small difference reliably. No measured-window GC events/after-GC heap samples occurred: zero observed events is not zero GC overhead or retained-memory proof. Candidate heap used at64 rose from399803456 to445940800bytes without comparable GC; no leak conclusion follows.

No clients were connected: actual packet traffic, rendered frame time, visible rotation and multiplayer behavior were not measured. Correctness tests separately assert motion-only payloads and no unchanged notification generation. Graph/frontier/read/queue/cache budgets are tested, but factories with thousands of components, long-term churn, live client chunk streaming and long-session memory retention still need profiling. Do not use these results to declare the playable alpha ready.

Earlier diagnostic attempts were discarded: an empty-world sleep fixture produced no flour; another reused a world containing old fixtures. The table uses only the two clean-world runs with ticking guards, cleared inventories and all mills producing output. Raw reports/JFR/worlds remain ignored.


## Alpha follow-up:full stacks, non-requesting reads and CPU scopes

The JDK print tool had been truncating stack output to its default5frames. Analysis now explicitly requests64 and correlates server samples/VM events with the interval preceding each custom end-of-tick event. Full-stack review of the original candidate shows234.570/213.977ms spikes at zero mechanisms in vanilla chunk lookup/spawn bookkeeping. At64 components the171.110ms tick sampled weather/shuffle/chunk work; the117.672ms tick also sampled our redstone/fluid calls. Sampling alone does not apportion the entire delay.

A600warmup/600measurement follow-up found a real inspection contract problem:our LoadedSignals call chain could reach ServerChunkCache.getChunkFutureMainThread / GenerationChunkHolder.scheduleChunkGenerationTask through Level.getBlockState despite a preceding hasChunkAt check. Reviewing pinned1.21.1 sources established that the guard checks tickets rather than FULL readiness. MechanicalWorldAccess now uses non-requesting getChunkNow and direct loaded LevelChunk reads, including manager callbacks. This prevents the inspection path from requesting/waiting for chunk readiness. Another cache sample walked HashMap.Long collision trees for packed chunk keys; that index now uses Minecraft's existing fastutil primitive-long map. No new mod dependency, recipe, persistence-format or graph-budget change was introduced.

Two fresh flat-world runs used the same seed12345,4CPU JVM,2GiB heap, spawn radius4, view/simulation distance4, no graphical clients/other builds during measurements,600tick warmup and600measured ticks/phase. The first used gameplay f26e6d8 plus CPU diagnostics; the second additionally used the loaded-access/index fix. CPU-read/wall-end ordering was corrected in the diagnostic timer between runs so its enclosing event-pair wall window includes the CPU read. This does not change the vanilla tick-ring sample or game workload. Short single runs on shared hardware are not statistical speedup evidence.

|Components|Before mean/p99/max ms|After mean/p99/max ms|Ticks >50ms before/after|After overworld post-bus mean ms|After scans /max scans per tick|
|---|---|---|---|---|---|
|0|1.529 /20.494 /66.433|1.354 /20.258 /62.353|5 /1|0.041|0 /0|
|16|1.846 /7.422 /528.674|0.964 /7.361 /52.798|1 /1|0.089|480 /6|
|64|3.736 /52.284 /425.213|1.240 /11.883 /104.555|7 /2|0.146|1680 /8|

Both runs completed:true and every mill produced flour:112 at16 components and448 at64 over the longer60s warmup+measurement workload. All declared scan budgets held. At64, the post-bus span containing the network scheduler averaged0.146ms, with30.571ms maximum wall and25.479ms maximum server-thread CPU. This includes other post listeners and measurement overhead; it is not an exact isolated MechanicalNetworks.process timer.

The after-run still had104.555ms and96.891ms whole ticks at64 components. Their post-bus spans were0.0176/0.0157ms, so that segment did not dominate those two delays. The96.891ms window sampled vanilla ChunkHolder.getTickingChunk / ServerChunkCache.tickChunks; the104.555ms window contained no useful server execution sample, so its precise cause remains unresolved. Do not declare all server lag fixed. CPU measured between Pre/Post callbacks omits dispatch before the Pre callback and is not identical to the vanilla tick-ring interval; no CPU/ring percentages are claimed.

At64 the after-run's weighted allocation samples totalled36.95MiB per measured30s. No measured-window GC events were recorded. These are sampling/short-run observations, not exact allocation counters, retained-memory evidence or a long-term stability test. Large factories, physical GPU rendering and online multiplayer remain outside this profile.

Reproduce the follow-up with a fresh disposable marked world using `./gradlew runMechanicalProfile -PprofileWarmupTicks=600 -PprofileLabel=alpha-loaded -PprofileRevision=<revision>`, then analyze the complete report with scripts/analyze-mechanical-profile.py. New recordings disable initial environment/system-property/JVM command-line snapshots; curated whitelisted settings remain in the JSON report. Raw files remain ignored.
