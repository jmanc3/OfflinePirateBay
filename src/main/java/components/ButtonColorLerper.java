package components;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class ButtonColorLerper {
    public static Color baseColor = new Color(.17, .22, .26, 1);
    public static Color hoveringColor = new Color(0.0, .42, .49, 1);
    public static Color clickedColor = new Color(.07, .52, .59, 1);

    public static void lerp(Button button, Text statusText, String message) {
        setBackground(button, baseColor);

        Animation colorTransitionAnimation = new Transition() {
            {
                setCycleDuration(Duration.millis(180));
                setAutoReverse(true);
            }

            @Override
            protected void interpolate(double frac) {
                int cycleCount = getCycleCount();
                if (cycleCount == 0) {
                    Color interpolate = baseColor.interpolate(hoveringColor, frac);

                    setBackground(button, interpolate);
                } else {
                    Color interpolate = baseColor.interpolate(hoveringColor, 1 - frac);
                    setBackground(button, interpolate);
                }
            }
        };

        colorTransitionAnimation.setAutoReverse(true);

        button.setOnMouseEntered(event -> {
            statusText.setText(message);
            colorTransitionAnimation.setCycleCount(0);
            colorTransitionAnimation.play();
        });
        button.setOnMouseExited(event -> {
            colorTransitionAnimation.setCycleCount(1);
            colorTransitionAnimation.play();
        });
        button.setOnMousePressed(event -> {
            setBackground(button, clickedColor);
        });
        button.setOnMouseClicked(event -> {
            setBackground(button, hoveringColor);
        });
    }

    private static void setBackground(Button button, Color color) {
        button.setStyle("-fx-background-color: " + colorToHex(color));
    }

    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", ((int) (color.getRed() * 255)), ((int) (color.getGreen() * 255)), ((int) (color.getBlue() * 255)));
    }
}
