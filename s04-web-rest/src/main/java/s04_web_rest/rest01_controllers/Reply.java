package s04_web_rest.rest01_controllers;

/**
 * Reply = odpowiedź. Mówi, KTÓRA metoda kontrolera obsłużyła zapytanie — dzięki temu w teście i w pliku .http widać,
 * jak DispatcherServlet wybrał metodę. JSON: {"handler":"ProductController.all","info":"lista produktów"}.
 */
public record Reply(String handler, String info) {
}
