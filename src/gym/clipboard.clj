(ns gym.core
  "Functions to interact with the system clipboard.
   Like `pbcopy` and `pbpaste`, but for the repl"
  (:import [java.awt.datatransfer DataFlavor StringSelection]))

(defn- clipboard
  "Grab a reference to the System clipboard"
  []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn clipboard->str
  "Write the contents of the system clipboard to a string, and return that string"
  []
  (.getTransferData (.getContents (clipboard) nil) (DataFlavor/stringFlavor)))

(defn str->clipboard
  "Write the string `text` to the system clipboard and return nil."
  [text]
  (let [selection (StringSelection. text)]
    (.setContents (clipboard) selection selection)))

(comment
  (str->clipboard "ooOOooOO I'm in your clipboard")
  (clipboard->str))
