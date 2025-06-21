package com.insert.hivedocs.data

object KnowledgeBase {
    val qna: Map<List<String>, String> = mapOf(
        listOf("o que é", "hivedocs", "sobre", "finalidade") to "O HiveDocs é uma plataforma colaborativa para submissão e avaliação de artigos acadêmicos. Usuários podem ler, enviar, avaliar e discutir o conteúdo.",
        listOf("quem pode usar", "tipos de usuário", "perfis") to "O aplicativo possui dois tipos de usuários: Usuários Comuns (leitores/autores) e Administradores (moderadores).",
        listOf("criar conta", "registrar", "cadastro") to "Na tela de login, você pode se registrar usando um e-mail e senha ou fazer login diretamente com sua conta Google.",
        listOf("ler artigos", "ver artigos", "lista") to "Na aba 'Artigos', você encontrará a lista de todos os trabalhos que já foram aprovados pela moderação. Toque em um para ver os detalhes.",
        listOf("enviar artigo", "submeter", "novo artigo") to "Use a aba 'Novo Artigo', preencha o formulário com Título, Autor, Ano, Resumo e o link (URL). Após o envio, seu artigo aguardará a aprovação de um administrador.",
        listOf("artigo não apareceu", "meu artigo sumiu", "status") to "Todo artigo enviado precisa ser aprovado por um administrador para aparecer na lista principal. Por favor, aguarde a revisão na aba 'Aprovações'.",
        listOf("link do artigo", "url", "fonte") to "O link para o artigo original, se fornecido pelo autor, está na página de detalhes do artigo e pode ser acessado clicando em 'Acessar artigo original'.",
        listOf("avaliar", "dar nota", "estrelas", "rating") to "Na página de detalhes de um artigo, use as estrelas clicáveis para dar uma nota, escreva um comentário e clique em 'Enviar Avaliação'.",
        listOf("editar avaliação", "excluir avaliação", "apagar comentário") to "Sim, você pode editar ou excluir sua própria avaliação. No seu comentário, clique no menu (três pontinhos) e escolha a opção desejada. Administradores podem excluir qualquer avaliação.",
        listOf("responder", "resposta") to "Sim, abaixo de cada avaliação há um botão 'Responder' para você postar uma resposta que ficará visível abaixo do comentário original.",
        listOf("administrador", "admin", "o que faz") to "Administradores têm acesso à aba 'Aprovações' para aprovar ou rejeitar artigos e podem excluir a avaliação ou comentário de qualquer usuário.",
        listOf("aprovar artigo", "rejeitar") to "A aprovação e rejeição de artigos é feita por administradores na aba 'Aprovações'. Cada item pendente terá os botões para a ação.",
        listOf("sair", "logout", "deslogar") to "Para sair da sua conta, vá para a aba 'Perfil' e clique no botão vermelho 'Sair da Conta'.",
        listOf("esqueci a senha", "mudar senha") to "A recuperação de senha para contas Google é gerenciada pela sua própria Conta Google. Para contas criadas com e-mail e senha, essa funcionalidade ainda não foi implementada.",
        listOf("como ser admin", "virar administrador") to "O status de administrador é concedido manualmente pela equipe de gerenciamento do HiveDocs para garantir a qualidade da moderação."
    )
}