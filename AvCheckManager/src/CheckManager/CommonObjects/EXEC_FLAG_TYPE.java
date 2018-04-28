package CheckManager.CommonObjects;


public enum EXEC_FLAG_TYPE
    {
        EXEC(0),
	SHELL(1),
        CONSOLE(2);
        private final int value;
        EXEC_FLAG_TYPE(int value){this.value = value;}
        public int value(){return this.value;}
    }
