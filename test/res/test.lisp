(import :test)

(test.test "Identity equality" (seq
    ((test.assert-eq false false))
    ((test.assert-eq true true))))

(test.test "Not behaves correctly" (seq
    ((test.assert-eq (not false) true))
    ((test.assert-eq (not true) false))))

(test.test "And behaves correctly" (seq
    ((test.assert-eq (& true true) true))
    ((test.assert-eq (& true false) false))
    ((test.assert-eq (& false true) false))
    ((test.assert-eq (& false false) false))))

(test.test "Or behaves correctly" (seq
    ((test.assert-eq (| true true) true))
    ((test.assert-eq (| true false) true))
    ((test.assert-eq (| false true) true))
    ((test.assert-eq (| false false) false))))

(test.test "Hashes" (seq
    (def funnyhash (hash.new :test1 1 :test2 2))
    ((test.assert-eq
        (hash.merge funnyhash (hash.new :test1 2))
        (hash.new :test1 2 :test2 2)))
    ((test.assert-eq funnyhash (hash.new :test1 1 :test2 2)))
    ((test.assert-eq (hash.get funnyhash :test1) 1))
    ((test.assert-eq (hash.get funnyhash :tesst3) nil))
    ))

(test.test "Hex literals" (seq
    ((test.assert-eqd 0x0 0 0.0001))
    ((test.assert-eqd 0xFF 255 0.0001))
    ((test.assert-eqd 0xFFFFFFFF 4294967295 0.0001))
    ))

