package s04_web_rest.rest04_json;

import java.util.ArrayList;
import java.util.List;

/**
 * Pracownik z DWUKIERUNKOWYM powiązaniem: szef zna podwładnych, a podwładny zna szefa. To typowy cykl referencji
 * (obiekt wskazuje na obiekt, który wskazuje z powrotem). Jackson nie umie zamienić go na JSON — patrz lekcja, sekcja 5.
 */
public class Employee {

    private final String name;
    private Employee boss;
    private final List<Employee> team = new ArrayList<>();

    public Employee(String name) {
        this.name = name;
    }

    public void addToTeam(Employee employee) {
        employee.boss = this;
        team.add(employee);
    }

    public String getName() {
        return name;
    }

    public Employee getBoss() {
        return boss;
    }

    public List<Employee> getTeam() {
        return team;
    }
}
