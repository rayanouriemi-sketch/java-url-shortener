package fr.rayan.shortener;

public final class Base62 {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Base62() {
    }

    public static String encode(long value) {
        if (value < 0) throw new IllegalArgumentException("valeur négative");
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(ALPHABET.charAt((int) (value % 62)));
            value /= 62;
        } while (value > 0);
        return sb.reverse().toString();
    }

    public static long decode(String text) {
        if (text == null || text.isEmpty()) throw new IllegalArgumentException("texte vide");
        long value = 0;
        for (char c : text.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) throw new IllegalArgumentException("caractère invalide : " + c);
            value = Math.addExact(Math.multiplyExact(value, 62), digit);
        }
        return value;
    }
}
