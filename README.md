# RouteMapper
### Ustawienie połączenia z lokalnym serwerem
Aplikacja łączy się z serwerem w sieci lokalnej. Należy ustawić String url znajdujący się w com.example.routemapper.network.WebClient na adres serwera z portem 8080.  
Przykładowo ``private var url: String = "http://192.168.1.49:8080/"``

### Obsługa aplikacji.
Aby zacząć śledzić kroki należy wybrać punkt startowy przez wskazanie punktu na mapie lub kliknięcie przycisku do lokalizowania. ![lokalizacja_screen](https://github.com/MDG369/RouteMapper/assets/73025866/a8538a7a-907d-4508-b1d0-9d64b49327ee)  
Po wybraniu lokalizacji należy przycisnąć przycisk znajdujący się w prawym dolnym roku ekranu.  
 ![start_btn_screen](https://github.com/MDG369/RouteMapper/assets/73025866/151286f5-f6e9-4c55-ab3b-d706b8335e6b)
Jeśli połączenie z serwerem działa powinien wyświetlić się komunikat z liczbą będącą id użytkownika.  
![userid_screen](https://github.com/MDG369/RouteMapper/assets/73025866/b6bd3319-7222-4e2b-bf9e-2395f2e0b28e)  
Śledzenie można zakończyć klikając ten sam przycisk. Można wtedy wybrać nowy punkt startowy i powtórzyć proces.
