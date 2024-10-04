pacakge org.example.practice1;

abstract class Race {
    protected Profession profession;

    public Race(Profession profession) {
        this.profession = profession;
    }

    abstract String attack();
    abstract String defend();
    abstract String useSpecial();
}
