import { motion } from 'framer-motion';
import { useCardDetection } from '../../hooks/useCardDetection';
import { maskCardNumber, formatExpiry } from '../../lib/luhn';

interface CardPreviewProps {
  cardholderName: string;
  cardNumber: string;
  expiry: string;
  cvv: string;
  isCvvFocused: boolean;
}

const cardBrands = {
  visa: (
    <svg viewBox="0 0 48 32" className="h-8 w-auto">
      <path fill="#1434CB" d="M19.5 22.5h-3l1.5-9h3l-1.5 9zm10.5-9.5c-.5-.2-1.3-.5-2.3-.5-2.5 0-4.3 1.3-4.3 3.2 0 1.5 1.3 2.3 2.3 2.8.9.5 1.2.7 1.2 1.2 0 .6-.7 1-1.4 1-1.6 0-2.5-.8-2.5-.8l-.5 1.4s1 .8 3 .8c2.6 0 4.4-1.3 4.4-3.4 0-1.2-.8-2.1-2.3-2.8-.9-.5-1.5-.7-1.5-1.2 0-.4.4-.9 1.4-.9.8 0 1.8.4 2.4.6l.6-1.2zm6.5-.5h-2.3c-.7 0-1.3.3-1.6 1l-4 9.5h3l.6-1.5h3.5l.3 1.5h2.7l-2.2-9.5zm-3.4 5.7l1.1-2.8c.2-.5.4-1.2.4-1.2l.1 1.4 1 2.6h-2.6zm-26.7 3.8L10 6.5h2.7l-1.6 9.5h-2.7l-.3-1.8h-2L5.8 22.5H3l3.5-20h3.5l1.6 9.5h2.7L14.3 5h-3z"/>
      <path fill="#1434CB" d="M24 6.5h-2.3l-1.6 9.5h2.3l1.6-9.5z"/>
    </svg>
  ),
  mastercard: (
    <svg viewBox="0 0 48 32" className="h-8 w-auto">
      <circle fill="#EB001B" cx="18" cy="16" r="8"/>
      <circle fill="#F79E1B" cx="30" cy="16" r="8"/>
    </svg>
  ),
  amex: (
    <svg viewBox="0 0 48 32" className="h-8 w-auto">
      <path fill="#006FCF" d="M24 6.5h-7l-1 5h-4l-1-5H6l4 22h4l1-5h3l1 5h4l4-22z"/>
      <path fill="#FFF" d="M18 11.5h-2v2h2v-2zm0 4h-2v2h2v-2z"/>
    </svg>
  ),
  rupay: (
    <svg viewBox="0 0 48 32" className="h-8 w-auto">
      <circle fill="#E60000" cx="24" cy="16" r="10"/>
      <path fill="#FFF" d="M24 10c-1.5 2-2.5 4-2.5 6s1 4 2.5 6c1.5-2 2.5-4 2.5-6s-1-4-2.5-6z"/>
    </svg>
  ),
  unknown: (
    <svg viewBox="0 0 48 32" className="h-8 w-auto opacity-50">
      <rect fill="#94A3B8" x="4" y="8" width="40" height="20" rx="2"/>
    </svg>
  ),
};

export function CardPreview({ 
  cardholderName, 
  cardNumber, 
  expiry, 
  cvv, 
  isCvvFocused 
}: CardPreviewProps) {
  const { brand } = useCardDetection(cardNumber);
  const maskedNumber = maskCardNumber(cardNumber);
  const formattedExpiry = formatExpiry(expiry);

  return (
    <div className="relative w-full max-w-sm mx-auto perspective-1000">
      <motion.div
        className="relative w-full h-52 rounded-xl bg-gradient-to-br from-gray-800 via-gray-900 to-black shadow-2xl"
        animate={{ rotateY: isCvvFocused ? 180 : 0 }}
        transition={{ duration: 0.6, ease: 'easeInOut' }}
        style={{ transformStyle: 'preserve-3d' }}
      >
        <div 
          className="absolute inset-0 w-full h-full backface-hidden"
          style={{ backfaceVisibility: 'hidden' }}
        >
          <div className="absolute top-4 right-4">
            {cardBrands[brand]}
          </div>
          
          <div className="absolute bottom-16 left-6">
            <p className="font-mono text-xl tracking-widest text-white">
              {maskedNumber || '•••• •••• •••• ••••'}
            </p>
          </div>
          
          <div className="absolute bottom-6 left-6 right-6 flex justify-between items-end">
            <div>
              <p className="text-xs text-gray-400 uppercase tracking-wider">Card Holder</p>
              <p className="text-sm text-white font-medium uppercase">
                {cardholderName || 'YOUR NAME'}
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-400 uppercase tracking-wider">Expires</p>
              <p className="text-sm text-white font-mono">
                {formattedExpiry || 'MM/YY'}
              </p>
            </div>
          </div>
        </div>

        <div 
          className="absolute inset-0 w-full h-full backface-hidden bg-gradient-to-br from-gray-800 via-gray-900 to-black rounded-xl"
          style={{ 
            backfaceVisibility: 'hidden', 
            transform: 'rotateY(180deg)' 
          }}
        >
          <div className="absolute top-8 left-0 right-0 h-8 bg-gray-700 mx-4" />
          <div className="absolute bottom-16 left-6 right-6">
            <p className="text-xs text-gray-400 uppercase tracking-wider">CVV</p>
            <p className="text-sm text-gray-300 font-mono bg-gray-800 px-2 py-1 rounded mt-1 inline-block">
              {cvv || '•••'}
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
