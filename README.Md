# Cinema Booking System

System rezerwacji biletów kinowych wykorzystujący komunikację UDP.

## Opis projektu

Aplikacja umożliwia:

- Przeglądanie dostępnych filmów i seansów
- Sprawdzanie dostępności miejsc
- Dokonywanie rezerwacji
- Anulowanie rezerwacji

System składa się z:

- Serwera UDP obsługującego żądania klientów
- Klienta z konsolowym interfejsem użytkownika
- Bazy danych H2 do przechowywania danych

## Struktura projektu

- `models` - Klasy reprezentujące obiekty domeny (filmy, sale, seanse, rezerwacje)
- `dao` - Warstwa dostępu do danych, obsługa bazy danych
- `UDPServer` - Serwer obsługujący żądania klientów
- `UDPClient` - Aplikacja kliencka

## Uruchomienie projektu

### Uruchomienie serwera

```bash
java -cp target/cinema-booking-system-1.0-SNAPSHOT.jar UDPServer [port]
```

Domyślny port: 9876

### Uruchomienie klienta

```bash
java -cp target/cinema-booking-system-1.0-SNAPSHOT.jar UDPClient [host] [port]
```

Domyślny host: localhost
Domyślny port: 9876

## Użyte technologie

- Java
- Protokół UDP do komunikacji
- Baza danych H2
- Maven
