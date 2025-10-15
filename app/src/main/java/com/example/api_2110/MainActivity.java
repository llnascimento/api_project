package com.example.api_2110;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText searchInput;
    private MaterialButton btnSearch;
    private ImageView pokemonImage;
    private TextView pokemonName;
    private TextView pokemonType;
    private TextView pokemonId;
    private ProgressBar hpBar;
    private ProgressBar attackBar;
    private ProgressBar defenseBar;
    private ProgressBar speedBar;
    private TextView hpValue;
    private TextView attackValue;
    private TextView defenseValue;
    private TextView speedValue;
    private TextView weightValue;
    private ProgressBar loadingIndicator;
    private View pokemonContent;

    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initializeViews();
        setupListeners();

        // Carregar Bulbasaur por padrão
        searchPokemon("bulbasaur");
    }

    private void initializeViews() {
        searchInput = findViewById(R.id.pokemonSearchInput);
        btnSearch = findViewById(R.id.btnSearchPokemon);
        pokemonImage = findViewById(R.id.pokemonImage);
        pokemonName = findViewById(R.id.pokemonName);
        pokemonType = findViewById(R.id.pokemonType);
        pokemonId = findViewById(R.id.pokemonId);
        hpBar = findViewById(R.id.hpBar);
        attackBar = findViewById(R.id.attackBar);
        defenseBar = findViewById(R.id.defenseBar);
        speedBar = findViewById(R.id.speedBar);
        hpValue = findViewById(R.id.hpValue);
        attackValue = findViewById(R.id.attackValue);
        defenseValue = findViewById(R.id.defenseValue);
        speedValue = findViewById(R.id.speedValue);
        weightValue = findViewById(R.id.weightValue);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        pokemonContent = findViewById(R.id.pokemonContent);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            String pokemonQuery = searchInput.getText().toString().trim().toLowerCase();
            if (!pokemonQuery.isEmpty()) {
                searchPokemon(pokemonQuery);
            } else {
                Toast.makeText(this, "Digite o nome ou ID do Pokémon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPokemon(String query) {
        showLoading(true);

        executorService.execute(() -> {
            try {
                Pokemon pokemon = fetchPokemonData(query);
                mainHandler.post(() -> {
                    updateUI(pokemon);
                    showLoading(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(
                            MainActivity.this,
                            "Erro ao buscar Pokémon: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private Pokemon fetchPokemonData(String query) throws Exception {
        String urlString = "https://pokeapi.co/api/v2/pokemon/" + query;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        JSONObject json = new JSONObject(response.toString());

        // Extrair dados do JSON
        int id = json.getInt("id");
        String name = json.getString("name");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        double weight = json.getInt("weight") / 10.0; // Converter para kg

        // Imagem
        JSONObject sprites = json.getJSONObject("sprites");
        String imageUrl = sprites.optString("front_default", "");

        // Tipos
        JSONArray typesArray = json.getJSONArray("types");
        List<String> types = new ArrayList<>();
        for (int i = 0; i < typesArray.length(); i++) {
            JSONObject typeObj = typesArray.getJSONObject(i);
            String typeName = typeObj.getJSONObject("type").getString("name");
            typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
            types.add(typeName);
        }

        // Stats
        JSONArray statsArray = json.getJSONArray("stats");
        int hp = 0;
        int attack = 0;
        int defense = 0;
        int speed = 0;

        for (int i = 0; i < statsArray.length(); i++) {
            JSONObject statObj = statsArray.getJSONObject(i);
            String statName = statObj.getJSONObject("stat").getString("name");
            int baseStat = statObj.getInt("base_stat");

            switch (statName) {
                case "hp":
                    hp = baseStat;
                    break;
                case "attack":
                    attack = baseStat;
                    break;
                case "defense":
                    defense = baseStat;
                    break;
                case "speed":
                    speed = baseStat;
                    break;
            }
        }

        return new Pokemon(id, name, types, imageUrl, hp, attack, defense, speed, weight);
    }

    private void updateUI(Pokemon pokemon) {
        pokemonName.setText(pokemon.getName());
        pokemonId.setText(String.format("#%03d", pokemon.getId()));
        pokemonType.setText(String.join(" / ", pokemon.getTypes()));

        // Carregar imagem com Glide
        if (!pokemon.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(pokemon.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(pokemonImage);
        }

        // Atualizar stats
        hpBar.setProgress(pokemon.getHp());
        hpValue.setText(String.valueOf(pokemon.getHp()));

        attackBar.setProgress(pokemon.getAttack());
        attackValue.setText(String.valueOf(pokemon.getAttack()));

        defenseBar.setProgress(pokemon.getDefense());
        defenseValue.setText(String.valueOf(pokemon.getDefense()));

        speedBar.setProgress(pokemon.getSpeed());
        speedValue.setText(String.valueOf(pokemon.getSpeed()));

        weightValue.setText(pokemon.getWeight() + " kg");
    }

    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        pokemonContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        btnSearch.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Classe Pokemon
    public static class Pokemon {
        private int id;
        private String name;
        private List<String> types;
        private String imageUrl;
        private int hp;
        private int attack;
        private int defense;
        private int speed;
        private double weight;

        public Pokemon(int id, String name, List<String> types, String imageUrl,
                       int hp, int attack, int defense, int speed, double weight) {
            this.id = id;
            this.name = name;
            this.types = types;
            this.imageUrl = imageUrl;
            this.hp = hp;
            this.attack = attack;
            this.defense = defense;
            this.speed = speed;
            this.weight = weight;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public List<String> getTypes() { return types; }
        public String getImageUrl() { return imageUrl; }
        public int getHp() { return hp; }
        public int getAttack() { return attack; }
        public int getDefense() { return defense; }
        public int getSpeed() { return speed; }
        public double getWeight() { return weight; }
    }
}