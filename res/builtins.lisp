(defun comment (...) ((pure nil)))
(comment "comment is a noop function for documentation")


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

