(core.defun comment (...) ((core.pure core.nil)))
(comment "comment is a noop function for documentation")
(export comment)

(comment "Re-exports from core")
(core.def def core.def)
(def defun core.defun)
(def if core.if)
(def nil core.nil)
(def stringify core.tostring)
(def pure core.pure)
(def lambda core.lambda)
(def seq core.seq)
(def import core.import)
(def debuglog core.debuglog)
(export def defun if nil stringify pure lambda seq import debuglog)

(comment "Re-exports from arithmetic")
(def + core.arith.add)
(def / core.arith.div)
(def * core.arith.mul)
(def - core.arith.sub)
(def = core.arith.eq)
(def lt core.arith.less)
(export + / * - = lt)

(comment "comparisons")
(defun gt (l r) (lt r l))
(export gt)


(comment "if! a strict version of a regular if, meaning it evaluates both the falsy and the truthy case, instead of only one.")
(defun if! (cond ifTrue ifFalse) (if cond ifTrue ifFalse))
(export if!)

(comment "return immediately returns a value where an invocation is expected")
(defun return (value) ((pure value)))
(export return)

(comment "noop is a do nothing function")
(defun noop () (return nil))
(export noop)

(comment "boolean atoms")
(def true :true)
(def false :false)
(export true false)

(comment "boolean operations. all of those are strict")
(defun | (l r) (if l true r))
(defun & (l r) (if l r false))
(defun not (v) (if v false true))
(defun ^ (l r) (if l (not r) r))
(export | & not ^)

(comment "Re-export hashes")
(def hash.new core.newhash)
(def hash.merge core.mergehash)
(def hash.get core.gethash)
(export hash.new hash.merge hash.get)


