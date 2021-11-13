package gui.managers;

import core.card.Card;
import core.card.CardColour;
import core.card.CardNumber;
import gui.Board;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

public class BoardManager {

    private final Board board;
    private final HashMap<String, BufferedImage> cardImages;

    public BoardManager(Board board) {
        this.board = board;
        cardImages = getAllCardImages();
    }

    private HashMap<String, BufferedImage> getAllCardImages() {
        HashMap<String, BufferedImage> images = new HashMap<>();
        String path;

        for (CardColour colour : CardColour.values()) {
            for (CardNumber number : CardNumber.values()) {
                path = colour.name() + "_" + number.name();
                images.put(path, getBufferedImage(path));
            }
        }

        return images;
    }

    private BufferedImage getBufferedImage(String path) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(Objects.requireNonNull(BoardManager.class.getResource("/images/cards/" + path + ".png")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return img;
    }

    //todo difference between console final table status and board frame
    public void putCardAtPosition(Card card, int position) {
        String path = card.colour().name() + "_" + card.number().name();
        board.setImageAtPosition(cardImages.get(path), position - 1);
    }

    public void showMessageDialog(String s) {
        JOptionPane.showMessageDialog(board,
                s,
                "Game Over",
                JOptionPane.PLAIN_MESSAGE);
    }
}