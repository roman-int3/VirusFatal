package CheckManager.CommonObjects;


public enum WIN_MSG
{
    BM_CLICK(0x00F5),
    WM_CLOSE(0x0010);
    private int code;
    WIN_MSG(int code){this.code = code;}
    public int code(){return code;}
}
