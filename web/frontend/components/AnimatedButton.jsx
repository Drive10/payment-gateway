import { motion } from "framer-motion";

export default function AnimatedButton({ text, onClick }) {
  return (
    <motion.button
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
      onClick={onClick}
      className="pay-btn"
    >
      {text}
    </motion.button>
  );
}
