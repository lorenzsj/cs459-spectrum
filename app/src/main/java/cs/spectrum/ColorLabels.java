package cs.spectrum;

public class ColorLabels {

    private int R;
    private int G;
    private int B;

    public ColorLabels()
    {
        R = 0;
        G = 0;
        B = 0;
    }

    public ColorLabels(int Red, int Green, int Blue)
    {
        R = Red;
        G = Green;
        B = Blue;
    }

    public void setRGB(int Red, int Green, int Blue)
    {
        R = Red;
        G = Green;
        B = Blue;
    }

    public String getR()
    {
        return R + "";
    }

    public String getG()
    {
        return G + "";
    }

    public String getB()
    {
        return B + "";
    }

    public String getColor()
    {
        if (red()) return "Red";
        if (green()) return "Green";
        if (blue()) return "Blue";
        if (yellow()) return "Yellow";
        if (cyan()) return "Cyan";
        if (orange()) return "Orange";
        if (purple()) return "Purple";
        if (brown()) return "Brown";
        if (pink()) return "Pink";
        if (grey()) return "Grey";
        if (isBW())
        {
            if (R < 100) return "Black";
            else return "White";
        }
        return "Unrecognized";
    }

    public boolean red()
    {
        if (isBW() || (grey())) return false;
        else if (R > G*2 && R > B*2.5 && R + G + B > 25) return true;
        else return false;
    }

    public boolean green()
    {
        if (isBW() || (grey())) return false;
        else if (R < 230 && G > R && G > B*1.15 && R + G + B > 50) return true;
        else return false;
    }

    public boolean blue()
    {
        if (isBW() || (grey())) return false;
        else if (B > R*2 && B > G*1.5 && R + G + B > 25) return true;
        else return false;
    }

    public boolean cyan()
    {
        if (isBW() || (grey())) return false;
        else if (R*2 < B + G && (B + G) > 250 && Math.abs(G - B) < 128) return true;
        else return false;
    }

    public boolean purple()
    {
        if (isBW() || (grey())) return false;
        else if (R > 50 && R > G*1.75 && R < (B + (255 - R)) && B < R*2) return true;
        else return false;
    }

    public boolean yellow()
    {
        if (isBW() || grey()) return false;
        else if (R > 180 && G > 160 && B < (R + G) / 2 && B < 216) return true;
        else return false;
    }

    public boolean orange()
    {
        if (isBW() || (grey())) return false;
        else if (R > 176 && G >= R/4 && G < 180 && B <= G && B < 228) return true;
        else return false;
    }

    public boolean pink()
    {
        if (isBW() || (grey())) return false;
        else if (R > 176 && G <= R - 8 && B > G) return true;
        else return false;
    }

    public boolean brown()
    {
        if (isBW() || (grey())) return false;
        else if (R < 220 && R > G - 32 && B < 150) return true;
        else return false;
    }

    public boolean grey()
    {
        if (isBW()) return false;
        else if (Math.abs(R - G) < 16 && Math.abs(R - B) < 16 && Math.abs(G - B) < 16) return true;
        else return false;
    }

    public boolean isBW()
    {
        if (((Math.abs((R + B) - 2*G) < 20 || Math.abs((R + G) - 2*B) < 20 || Math.abs((G + B) - 2*R) < 60) && (R + G + B < 100 || R + G + B > 700)) || R + G + B < 50) return true;
        else return false;
    }

    public boolean isColor()
    {
        if (!isBW() && (red() || blue() || green() || cyan() || purple() || yellow() || orange() || pink() || brown() || grey())) return true;
        else return false;
    }
}