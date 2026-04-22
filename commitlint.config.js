module.exports = {
    extends: ['@commitlint/config-conventional'],

    rules: {
        'type-enum': [
            2,
            'always',
            [
                'feat',
                'fix',
                'docs',
                'style',
                'refactor',
                'test',
                'chore',
                'ci',
                'perf',
                'build',
                'revert'
            ]
        ],

        // não enche o saco com caixa alta/baixa
        'subject-case': [0],

        // evita commit vazio tipo "fix:"
        'subject-empty': [2, 'never'],

        // evita commit sem tipo (seu erro anterior)
        'type-empty': [2, 'never'],

        // limite saudável (opcional mas bom)
        'header-max-length': [2, 'always', 100],
    },

    ignores: [
        // 🔥 CRÍTICO: evita quebrar CI com merges automáticos
        (message) => message.startsWith('Merge'),

        // 🔥 opcional: ignorar commits gerados automaticamente
        (message) => message.startsWith('Revert'),
    ],
};