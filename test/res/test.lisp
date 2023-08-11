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
