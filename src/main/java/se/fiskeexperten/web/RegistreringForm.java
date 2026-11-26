package se.fiskeexperten.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulärdata för "Skapa konto". */
public class RegistreringForm {

    @NotBlank(message = "Ange din e-postadress")
    @Email(message = "Ange en giltig e-postadress")
    @Size(max = 120, message = "Högst 120 tecken")
    private String email;

    @NotBlank(message = "Välj ett lösenord")
    @Size(min = 8, message = "Lösenordet måste vara minst 8 tecken")
    // BCrypt klarar högst 72 byte. Kontrollen sker här, före PasswordEncoder.encode() i controllern.
    @MaxUtf8Bytes(value = 72, message = "Lösenordet är för långt (högst 72 byte – å, ä och ö räknas som 2)")
    private String losenord;

    @NotBlank(message = "Skriv lösenordet igen")
    private String losenordIgen;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLosenord() { return losenord; }
    public void setLosenord(String losenord) { this.losenord = losenord; }

    public String getLosenordIgen() { return losenordIgen; }
    public void setLosenordIgen(String losenordIgen) { this.losenordIgen = losenordIgen; }
}
